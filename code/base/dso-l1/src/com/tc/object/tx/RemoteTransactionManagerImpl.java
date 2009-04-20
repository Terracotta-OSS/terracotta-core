/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;
import com.tc.util.SequenceID;
import com.tc.util.State;
import com.tc.util.TCAssertionError;
import com.tc.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

/**
 * Sends off committed transactions
 */
public class RemoteTransactionManagerImpl implements RemoteTransactionManager {

  private static final long                       FLUSH_WAIT_INTERVAL         = 15 * 1000;

  private static final int                        MAX_OUTSTANDING_BATCHES     = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getInt(
                                                                                          TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE);
  private static final long                       COMPLETED_ACK_FLUSH_TIMEOUT = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getLong(
                                                                                           TCPropertiesConsts.L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT);

  private static final State                      RUNNING                     = new State("RUNNING");
  private static final State                      PAUSED                      = new State("PAUSED");
  private static final State                      STOP_INITIATED              = new State("STOP-INITIATED");
  private static final State                      STOPPED                     = new State("STOPPED");

  private final Object                            lock                        = new Object();
  private final Map                               incompleteBatches           = new HashMap();
  private final HashMap                           lockFlushCallbacks          = new HashMap();

  private final Counter                           outstandingBatchesCounter;
  private final TransactionBatchAccounting        batchAccounting             = new TransactionBatchAccounting();
  private final LockAccounting                    lockAccounting              = new LockAccounting();
  private final TCLogger                          logger;
  private final long                              ackOnExitTimeout;

  private int                                     outStandingBatches          = 0;
  private State                                   status;
  private final SessionManager                    sessionManager;
  private final TransactionSequencer              sequencer;
  private final DSOClientMessageChannel           channel;
  private final Timer                             timer                       = new Timer(
                                                                                          "RemoteTransactionManager Flusher",
                                                                                          true);
  private final RemoteTransactionManagerTimerTask remoteTxManagerTimerTask;

  private final GroupID                           groupID;

  public RemoteTransactionManagerImpl(final GroupID groupID, final TCLogger logger,
                                      final TransactionBatchFactory batchFactory,
                                      final TransactionIDGenerator transactionIDGenerator,
                                      final SessionManager sessionManager, final DSOClientMessageChannel channel,
                                      final Counter outstandingBatchesCounter, final Counter pendingBatchesSize,
                                      final SampledRateCounter transactionSizeCounter,
                                      final SampledRateCounter transactionsPerBatchCounter,
                                      final long ackOnExitTimeoutMs) {
    this.groupID = groupID;
    this.logger = logger;
    this.sessionManager = sessionManager;
    this.channel = channel;
    this.status = RUNNING;
    this.ackOnExitTimeout = ackOnExitTimeoutMs;
    this.sequencer = new TransactionSequencer(groupID, transactionIDGenerator, batchFactory, this.lockAccounting,
                                              pendingBatchesSize, transactionSizeCounter, transactionsPerBatchCounter);
    this.remoteTxManagerTimerTask = new RemoteTransactionManagerTimerTask();
    this.timer.schedule(this.remoteTxManagerTimerTask, COMPLETED_ACK_FLUSH_TIMEOUT, COMPLETED_ACK_FLUSH_TIMEOUT);
    this.outstandingBatchesCounter = outstandingBatchesCounter;
  }

  public void pause(final NodeID remote, final int disconnected) {
    synchronized (this.lock) {
      this.remoteTxManagerTimerTask.reset();
      if (isStoppingOrStopped()) { return; }
      if (this.status == PAUSED) { throw new AssertionError("Attempt to pause while already paused state."); }
      this.status = PAUSED;
    }
  }

  public void unpause(final NodeID remote, final int disconnected) {
    synchronized (this.lock) {
      if (isStoppingOrStopped()) { return; }
      if (this.status != PAUSED) { throw new AssertionError("Attempt to unpause while not in paused state."); }
      resendOutstanding();
      this.status = RUNNING;
      this.lock.notifyAll();
    }
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    synchronized (this.lock) {
      if (this.status != PAUSED) { throw new AssertionError("Attempting to handshake while not in paused state."); }
      handshakeMessage.addTransactionSequenceIDs(getTransactionSequenceIDs());
      handshakeMessage.addResentTransactionIDs(getResentTransactionIDs());
    }
  }

  /**
   * This is for testing only.
   */
  void clear() {
    synchronized (this.lock) {
      this.sequencer.clear();
      this.incompleteBatches.clear();
    }
  }

  /**
   * This is for testing only.
   */
  public int getMaxOutStandingBatches() {
    return MAX_OUTSTANDING_BATCHES;
  }

  public void stopProcessing() {
    this.sequencer.shutdown();
    this.channel.close();
  }

  public void stop() {
    final long start = System.currentTimeMillis();
    this.logger.debug("stop() is called on " + System.identityHashCode(this));
    synchronized (this.lock) {
      this.status = STOP_INITIATED;

      sendBatches(true, "stop()");

      long pollInteval = (this.ackOnExitTimeout > 0) ? (this.ackOnExitTimeout / 10) : (30 * 1000);
      long t0 = System.currentTimeMillis();
      if (this.incompleteBatches.size() != 0) {
        try {
          int incompleteBatchesCount = 0;
          LossyTCLogger lossyLogger = new LossyTCLogger(this.logger, 5, LossyTCLogger.COUNT_BASED);
          while ((this.status != STOPPED)
                 && ((this.ackOnExitTimeout <= 0) || (t0 + this.ackOnExitTimeout) > System.currentTimeMillis())) {
            if (incompleteBatchesCount != this.incompleteBatches.size()) {
              lossyLogger.info("stop(): incompleteBatches.size() = "
                               + (incompleteBatchesCount = this.incompleteBatches.size()));
            }
            this.lock.wait(pollInteval);
          }
        } catch (InterruptedException e) {
          this.logger.warn("stop(): Interrupted " + e);
        }
        if (this.status != STOPPED) {
          this.logger.error("stop() : There are still UNACKed Transactions! incompleteBatches.size() = "
                            + this.incompleteBatches.size());
        }
      }
      this.status = STOPPED;
    }
    this.logger.info("stop(): took " + (System.currentTimeMillis() - start) + " millis to complete");
  }

  public void flush(final LockID lockID) {
    final long start = System.currentTimeMillis();
    long lastPrinted = 0;
    boolean isInterrupted = false;
    Collection c;
    synchronized (this.lock) {
      while ((!(c = this.lockAccounting.getTransactionsFor(lockID)).isEmpty())) {
        try {
          this.lock.wait(FLUSH_WAIT_INTERVAL);
          long now = System.currentTimeMillis();
          if ((now - start) > FLUSH_WAIT_INTERVAL && (now - lastPrinted) > FLUSH_WAIT_INTERVAL / 3) {
            this.logger.info("Flush for " + lockID + " took longer than: " + (FLUSH_WAIT_INTERVAL / 1000)
                             + " sec. Took : " + (now - start) + " ms. # Transactions not yet Acked = " + c.size()
                             + "\n");
            lastPrinted = now;
          }
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  /* This does not block unlike flush() */
  public boolean isTransactionsForLockFlushed(final LockID lockID, final LockFlushCallback callback) {
    synchronized (this.lock) {

      if ((this.lockAccounting.getTransactionsFor(lockID)).isEmpty()) {
        // All transactions are flushed !
        return true;
      } else {
        // register for call back
        Object prev = this.lockFlushCallbacks.put(lockID, callback);
        if (prev != null) {
          // Will this scenario come up in server restart scenario ? It should as we check for greediness in the Lock
          // Manager before making this call
          throw new TCAssertionError("There is already a registered call back on Lock Flush for this lock ID - "
                                     + lockID);
        }
        return false;
      }
    }
  }

  public void commit(final ClientTransaction txn) {
    if (!txn.hasChangesOrNotifies() && txn.getDmiDescriptors().isEmpty() && txn.getNewRoots().isEmpty()) { throw new AssertionError(
                                                                                                                                    "Attempt to commit an empty transaction."); }
    if (!txn.getTransactionID().isNull()) { throw new AssertionError(
                                                                     "Transaction already committed as TransactionID is already assigned"); }
    long start = System.currentTimeMillis();

    this.sequencer.addTransaction(txn);

    long diff = System.currentTimeMillis() - start;
    if (diff > 1000) {
      this.logger.info(txn.getTransactionID() + " : Took more than 1000ms to add to sequencer  : " + diff + " ms");
    }

    synchronized (this.lock) {
      if (isStoppingOrStopped()) {
        // Send now if stop is requested
        sendBatches(true, "commit() : Stop initiated.");
      }
      waitUntilRunning();
      sendBatches(false);
    }
  }

  private void sendBatches(final boolean ignoreMax) {
    sendBatches(ignoreMax, null);
  }

  private void sendBatches(final boolean ignoreMax, final String message) {
    ClientTransactionBatch batch;
    while ((ignoreMax || canSendBatch()) && (batch = this.sequencer.getNextBatch()) != null) {
      if (message != null) {
        this.logger.debug(message + " : Sending batch containing " + batch.numberOfTxnsBeforeFolding() + " txns");
      }
      sendBatch(batch, true);
    }
  }

  private boolean canSendBatch() {
    return (this.outStandingBatches < MAX_OUTSTANDING_BATCHES);
  }

  void resendOutstanding() {
    synchronized (this.lock) {
      this.logger.debug("resendOutstanding()...");
      this.outStandingBatches = 0;
      this.outstandingBatchesCounter.setValue(0);
      List toSend = this.batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      if (toSend.size() == 0) {
        sendBatches(false, " resendOutstanding()");
      } else {
        for (Iterator i = toSend.iterator(); i.hasNext();) {
          TxnBatchID id = (TxnBatchID) i.next();
          ClientTransactionBatch batch = (ClientTransactionBatch) this.incompleteBatches.get(id);
          if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
          this.logger.debug("Resending outstanding batch: " + id + ", "
                            + batch.addTransactionIDsTo(new LinkedHashSet()));
          sendBatch(batch, false);
        }
      }
    }
  }

  List getTransactionSequenceIDs() {
    synchronized (this.lock) {
      ArrayList sequenceIDs = new ArrayList();
      // Add list of SequenceIDs that are going to be resent
      List toSend = this.batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      for (Iterator i = toSend.iterator(); i.hasNext();) {
        TxnBatchID id = (TxnBatchID) i.next();
        ClientTransactionBatch batch = (ClientTransactionBatch) this.incompleteBatches.get(id);
        if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
        batch.addTransactionSequenceIDsTo(sequenceIDs);
      }
      // Add Last next
      SequenceID currentBatchMinSeq = this.sequencer.getNextSequenceID();
      Assert.assertFalse(SequenceID.NULL_ID.equals(currentBatchMinSeq));
      sequenceIDs.add(currentBatchMinSeq);
      return sequenceIDs;
    }
  }

  List getResentTransactionIDs() {
    synchronized (this.lock) {
      ArrayList txIDs = new ArrayList();
      // Add list of TransactionIDs that are going to be resent
      List toSend = this.batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      for (Iterator i = toSend.iterator(); i.hasNext();) {
        TxnBatchID id = (TxnBatchID) i.next();
        ClientTransactionBatch batch = (ClientTransactionBatch) this.incompleteBatches.get(id);
        if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
        batch.addTransactionIDsTo(txIDs);
      }
      return txIDs;
    }
  }

  private boolean isStoppingOrStopped() {
    return this.status == STOP_INITIATED || this.status == STOPPED;
  }

  private void sendBatch(final ClientTransactionBatch batchToSend, final boolean account) {
    synchronized (this.lock) {
      if (account) {
        if (this.incompleteBatches.put(batchToSend.getTransactionBatchID(), batchToSend) != null) {
          // formatting
          throw new AssertionError("Batch has already been sent!");
        }
        Collection txnIds = batchToSend.addTransactionIDsTo(new HashSet());
        this.batchAccounting.addBatch(batchToSend.getTransactionBatchID(), txnIds);

      }
      batchToSend.send();
      this.outStandingBatches++;
      this.outstandingBatchesCounter.increment();
    }
  }

  // XXX:: Currently server always sends NULL BatchID
  public void receivedBatchAcknowledgement(final TxnBatchID txnBatchID, final NodeID remoteNode) {
    synchronized (this.lock) {
      if (isStoppingOrStopped()) {
        this.logger.warn(this.status + " : Received ACK for batch = " + txnBatchID);
        this.lock.notifyAll();
        return;
      }

      waitUntilRunning();
      this.outStandingBatches--;
      this.outstandingBatchesCounter.decrement();
      sendBatches(false);
      this.lock.notifyAll();
    }
  }

  public TransactionBuffer receivedAcknowledgement(final SessionID sessionID, final TransactionID txID,
                                                   final NodeID remoteNode) {
    TransactionBuffer tb = null;
    Map callbacks;
    synchronized (this.lock) {
      // waitUntilRunning();
      if (!this.sessionManager.isCurrentSession(remoteNode, sessionID)) {
        this.logger.warn("Ignoring Transaction ACK for " + txID + " from previous session = " + sessionID);
        return tb;
      }

      Set completedLocks = this.lockAccounting.acknowledge(txID);

      TxnBatchID container = this.batchAccounting.getBatchByTransactionID(txID);
      if (!container.isNull()) {
        ClientTransactionBatch containingBatch = (ClientTransactionBatch) this.incompleteBatches.get(container);
        tb = containingBatch.removeTransaction(txID);
        TxnBatchID completed = this.batchAccounting.acknowledge(txID);
        if (!completed.isNull()) {
          this.incompleteBatches.remove(completed);
          if (this.status == STOP_INITIATED && this.incompleteBatches.size() == 0) {
            this.logger.debug("Received ACK for the last Transaction. Moving to STOPPED state.");
            this.status = STOPPED;
          }
        }
      } else {
        this.logger.fatal("No batch found for acknowledgement: " + txID + " The batch accounting is "
                          + this.batchAccounting);
        throw new AssertionError("No batch found for acknowledgement: " + txID);
      }
      this.lock.notifyAll();
      callbacks = getLockFlushCallbacks(completedLocks);
    }
    fireLockFlushCallbacks(callbacks);
    return tb;
  }

  private TransactionID getCompletedTransactionIDLowWaterMark() {
    synchronized (this.lock) {
      waitUntilRunning();
      return this.batchAccounting.getLowWaterMark();
    }
  }

  /*
   * Never fire callbacks while holding lock
   */
  private void fireLockFlushCallbacks(final Map callbacks) {
    if (callbacks.isEmpty()) { return; }
    for (Iterator i = callbacks.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      LockID lid = (LockID) e.getKey();
      LockFlushCallback callback = (LockFlushCallback) e.getValue();
      callback.transactionsForLockFlushed(lid);
    }
  }

  private Map getLockFlushCallbacks(final Set completedLocks) {
    Map callbacks = Collections.EMPTY_MAP;
    if (!completedLocks.isEmpty() && !this.lockFlushCallbacks.isEmpty()) {
      for (Iterator i = completedLocks.iterator(); i.hasNext();) {
        Object lid = i.next();
        Object callback = this.lockFlushCallbacks.remove(lid);
        if (callback != null) {
          if (callbacks == Collections.EMPTY_MAP) {
            callbacks = new HashMap();
          }
          callbacks.put(lid, callback);
        }
      }
    }
    return callbacks;
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (this.status != RUNNING) {
      try {
        this.lock.wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  /*
   * For Tests
   */
  TransactionBatchAccounting getBatchAccounting() {
    return this.batchAccounting;
  }

  private class RemoteTransactionManagerTimerTask extends TimerTask {

    private volatile TransactionID currentLWM = TransactionID.NULL_ID;

    @Override
    public void run() {
      try {
        TransactionID lwm = getCompletedTransactionIDLowWaterMark();
        if (lwm.isNull()) { return; }
        if (this.currentLWM.toLong() > lwm.toLong()) { throw new AssertionError(
                                                                                "Transaction Low watermark moved down from "
                                                                                    + this.currentLWM + " to " + lwm); }
        if (this.currentLWM.toLong() == lwm.toLong()) { return; }
        this.currentLWM = lwm;
        CompletedTransactionLowWaterMarkMessage ctm = RemoteTransactionManagerImpl.this.channel
            .getCompletedTransactionLowWaterMarkMessageFactory()
            .newCompletedTransactionLowWaterMarkMessage(RemoteTransactionManagerImpl.this.groupID);
        ctm.initialize(lwm);
        ctm.send();
      } catch (Exception e) {
        RemoteTransactionManagerImpl.this.logger.error("Error sending Low water mark : ", e);
        throw new AssertionError(e);
      }
    }

    public void reset() {
      this.currentLWM = TransactionID.NULL_ID;
    }
  }
}
