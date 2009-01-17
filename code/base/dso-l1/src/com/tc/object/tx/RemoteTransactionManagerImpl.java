/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
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
import com.tc.stats.counter.sampled.SampledCounter;
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
public class RemoteTransactionManagerImpl implements RemoteTransactionManager, ClientHandshakeCallback {

  private static final long                FLUSH_WAIT_INTERVAL         = 15 * 1000;

  private static final int                 MAX_OUTSTANDING_BATCHES     = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getInt(
                                                                                   TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE);
  private static final long                COMPLETED_ACK_FLUSH_TIMEOUT = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getLong(
                                                                                    TCPropertiesConsts.L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT);

  private long                             ackOnExitTimeout            = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getLong(
                                                                                    TCPropertiesConsts.L1_TRANSACTIONMANAGER_TIMEOUTFORACK_ONEXIT) * 1000;

  private static final State               RUNNING                     = new State("RUNNING");
  private static final State               PAUSED                      = new State("PAUSED");
  private static final State               STOP_INITIATED              = new State("STOP-INITIATED");
  private static final State               STOPPED                     = new State("STOPPED");

  private final Object                     lock                        = new Object();
  private final Map                        incompleteBatches           = new HashMap();
  private final HashMap                    lockFlushCallbacks          = new HashMap();

  private final Counter                    outstandingBatchesCounter;
  private final TransactionBatchAccounting batchAccounting             = new TransactionBatchAccounting();
  private final LockAccounting             lockAccounting              = new LockAccounting();
  private final TCLogger                   logger;

  private int                              outStandingBatches          = 0;
  private State                            status;
  private final SessionManager             sessionManager;
  private final TransactionSequencer       sequencer;
  private final DSOClientMessageChannel    channel;
  private final Timer                      timer                       = new Timer("RemoteTransactionManager Flusher",
                                                                                   true);

  private final GroupID                    groupID;

  public RemoteTransactionManagerImpl(GroupID groupID, TCLogger logger, final TransactionBatchFactory batchFactory,
                                      TransactionIDGenerator transactionIDGenerator, SessionManager sessionManager,
                                      DSOClientMessageChannel channel, Counter outstandingBatchesCounter,
                                      SampledCounter numTransactionCounter, SampledCounter numBatchesCounter,
                                      final SampledCounter batchSizeCounter, final Counter pendingBatchesSize) {
    this.groupID = groupID;
    this.logger = logger;
    this.sessionManager = sessionManager;
    this.channel = channel;
    this.status = RUNNING;
    this.sequencer = new TransactionSequencer(groupID, transactionIDGenerator, batchFactory, lockAccounting,
                                              numTransactionCounter, numBatchesCounter, batchSizeCounter,
                                              pendingBatchesSize);
    this.timer.schedule(new RemoteTransactionManagerTimerTask(), COMPLETED_ACK_FLUSH_TIMEOUT,
                        COMPLETED_ACK_FLUSH_TIMEOUT);
    this.outstandingBatchesCounter = outstandingBatchesCounter;
  }

  public void pause(NodeID remote, int disconnected) {
    synchronized (lock) {
      if (isStoppingOrStopped()) return;
      if (this.status == PAUSED) throw new AssertionError("Attempt to pause while already paused state.");
      this.status = PAUSED;
    }
  }

  public void unpause(NodeID remote, int disconnected) {
    synchronized (lock) {
      if (isStoppingOrStopped()) return;
      if (this.status != PAUSED) throw new AssertionError("Attempt to unpause while not in paused state.");
      resendOutstanding();
      this.status = RUNNING;
      lock.notifyAll();
    }
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    synchronized (lock) {
      if (this.status != PAUSED) throw new AssertionError("Attempting to handshake while not in paused state.");
      handshakeMessage.addTransactionSequenceIDs(getTransactionSequenceIDs());
      handshakeMessage.addResentTransactionIDs(getResentTransactionIDs());
    }
  }

  /**
   * This is for testing only.
   */
  void clear() {
    synchronized (lock) {
      sequencer.clear();
      incompleteBatches.clear();
    }
  }

  /**
   * This is for testing only.
   */
  public int getMaxOutStandingBatches() {
    return MAX_OUTSTANDING_BATCHES;
  }

  public void stopProcessing() {
    sequencer.shutdown();
    channel.close();
  }

  public void stop() {
    final long start = System.currentTimeMillis();
    logger.debug("stop() is called on " + System.identityHashCode(this));
    synchronized (lock) {
      this.status = STOP_INITIATED;

      sendBatches(true, "stop()");

      long pollInteval = (ackOnExitTimeout > 0) ? (ackOnExitTimeout / 10) : (30 * 1000);
      long t0 = System.currentTimeMillis();
      if (incompleteBatches.size() != 0) {
        try {
          int incompleteBatchesCount = 0;
          LossyTCLogger lossyLogger = new LossyTCLogger(logger, 5, LossyTCLogger.COUNT_BASED);
          while ((status != STOPPED)
                 && ((ackOnExitTimeout <= 0) || (t0 + ackOnExitTimeout) > System.currentTimeMillis())) {
            if (incompleteBatchesCount != incompleteBatches.size()) {
              lossyLogger.info("stop(): incompleteBatches.size() = "
                               + (incompleteBatchesCount = incompleteBatches.size()));
            }
            lock.wait(pollInteval);
          }
        } catch (InterruptedException e) {
          logger.warn("stop(): Interrupted " + e);
        }
        if (status != STOPPED) {
          logger.error("stop() : There are still UNACKed Transactions! incompleteBatches.size() = "
                       + incompleteBatches.size());
        }
      }
      this.status = STOPPED;
    }
    logger.info("stop(): took " + (System.currentTimeMillis() - start) + " millis to complete");
  }

  public void flush(LockID lockID) {
    final long start = System.currentTimeMillis();
    long lastPrinted = 0;
    boolean isInterrupted = false;
    Collection c;
    synchronized (lock) {
      while ((!(c = lockAccounting.getTransactionsFor(lockID)).isEmpty())) {
        try {
          lock.wait(FLUSH_WAIT_INTERVAL);
          long now = System.currentTimeMillis();
          if ((now - start) > FLUSH_WAIT_INTERVAL && (now - lastPrinted) > FLUSH_WAIT_INTERVAL / 3) {
            logger.info("Flush for " + lockID + " took longer than: " + (FLUSH_WAIT_INTERVAL / 1000) + " sec. Took : "
                        + (now - start) + " ms. # Transactions not yet Acked = " + c.size() + "\n");
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
  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    synchronized (lock) {

      if ((lockAccounting.getTransactionsFor(lockID)).isEmpty()) {
        // All transactions are flushed !
        return true;
      } else {
        // register for call back
        Object prev = lockFlushCallbacks.put(lockID, callback);
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

  public void commit(ClientTransaction txn) {
    if (!txn.hasChangesOrNotifies() && txn.getDmiDescriptors().isEmpty() && txn.getNewRoots().isEmpty()) throw new AssertionError(
                                                                                                                                  "Attempt to commit an empty transaction.");
    if (!txn.getTransactionID().isNull()) throw new AssertionError(
                                                                   "Transaction already committed as TransactionID is already assigned");
    long start = System.currentTimeMillis();

    sequencer.addTransaction(txn);

    long diff = System.currentTimeMillis() - start;
    if (diff > 1000) {
      logger.info(txn.getTransactionID() + " : Took more than 1000ms to add to sequencer  : " + diff + " ms");
    }

    synchronized (lock) {
      if (isStoppingOrStopped()) {
        // Send now if stop is requested
        sendBatches(true, "commit() : Stop initiated.");
      }
      waitUntilRunning();
      sendBatches(false);
    }
  }

  private void sendBatches(boolean ignoreMax) {
    sendBatches(ignoreMax, null);
  }

  private void sendBatches(boolean ignoreMax, String message) {
    ClientTransactionBatch batch;
    while ((ignoreMax || canSendBatch()) && (batch = sequencer.getNextBatch()) != null) {
      if (message != null) {
        logger.debug(message + " : Sending batch containing " + batch.numberOfTxnsBeforeFolding() + " txns");
      }
      sendBatch(batch, true);
    }
  }

  private boolean canSendBatch() {
    return (outStandingBatches < MAX_OUTSTANDING_BATCHES);
  }

  void resendOutstanding() {
    synchronized (lock) {
      logger.debug("resendOutstanding()...");
      outStandingBatches = 0;
      outstandingBatchesCounter.setValue(0);
      List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      if (toSend.size() == 0) {
        sendBatches(false, " resendOutstanding()");
      } else {
        for (Iterator i = toSend.iterator(); i.hasNext();) {
          TxnBatchID id = (TxnBatchID) i.next();
          ClientTransactionBatch batch = (ClientTransactionBatch) incompleteBatches.get(id);
          if (batch == null) throw new AssertionError("Unknown batch: " + id);
          logger.debug("Resending outstanding batch: " + id + ", " + batch.addTransactionIDsTo(new LinkedHashSet()));
          sendBatch(batch, false);
        }
      }
    }
  }

  List getTransactionSequenceIDs() {
    synchronized (lock) {
      ArrayList sequenceIDs = new ArrayList();
      // Add list of SequenceIDs that are going to be resent
      List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      for (Iterator i = toSend.iterator(); i.hasNext();) {
        TxnBatchID id = (TxnBatchID) i.next();
        ClientTransactionBatch batch = (ClientTransactionBatch) incompleteBatches.get(id);
        if (batch == null) throw new AssertionError("Unknown batch: " + id);
        batch.addTransactionSequenceIDsTo(sequenceIDs);
      }
      // Add Last next
      SequenceID currentBatchMinSeq = sequencer.getNextSequenceID();
      Assert.assertFalse(SequenceID.NULL_ID.equals(currentBatchMinSeq));
      sequenceIDs.add(currentBatchMinSeq);
      return sequenceIDs;
    }
  }

  List getResentTransactionIDs() {
    synchronized (lock) {
      ArrayList txIDs = new ArrayList();
      // Add list of TransactionIDs that are going to be resent
      List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      for (Iterator i = toSend.iterator(); i.hasNext();) {
        TxnBatchID id = (TxnBatchID) i.next();
        ClientTransactionBatch batch = (ClientTransactionBatch) incompleteBatches.get(id);
        if (batch == null) throw new AssertionError("Unknown batch: " + id);
        batch.addTransactionIDsTo(txIDs);
      }
      return txIDs;
    }
  }

  private boolean isStoppingOrStopped() {
    return status == STOP_INITIATED || status == STOPPED;
  }

  private void sendBatch(ClientTransactionBatch batchToSend, boolean account) {
    synchronized (lock) {
      if (account) {
        if (incompleteBatches.put(batchToSend.getTransactionBatchID(), batchToSend) != null) {
          // formatting
          throw new AssertionError("Batch has already been sent!");
        }
        Collection txnIds = batchToSend.addTransactionIDsTo(new HashSet());
        batchAccounting.addBatch(batchToSend.getTransactionBatchID(), txnIds);

      }
      batchToSend.send();
      outStandingBatches++;
      outstandingBatchesCounter.increment();
    }
  }

  // XXX:: Currently server always sends NULL BatchID
  public void receivedBatchAcknowledgement(TxnBatchID txnBatchID, NodeID remoteNode) {
    synchronized (lock) {
      if (status == STOP_INITIATED) {
        logger.warn(status + " : Received ACK for batch = " + txnBatchID);
        lock.notifyAll();
        return;
      }

      waitUntilRunning();
      outStandingBatches--;
      outstandingBatchesCounter.decrement();
      sendBatches(false);
      lock.notifyAll();
    }
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID txID, NodeID remoteNode) {
    Map callbacks;
    synchronized (lock) {
      // waitUntilRunning();
      if (!sessionManager.isCurrentSession(remoteNode, sessionID)) {
        logger.warn("Ignoring Transaction ACK for " + txID + " from previous session = " + sessionID);
        return;
      }

      Set completedLocks = lockAccounting.acknowledge(txID);

      TxnBatchID container = batchAccounting.getBatchByTransactionID(txID);
      if (!container.isNull()) {
        ClientTransactionBatch containingBatch = (ClientTransactionBatch) incompleteBatches.get(container);
        containingBatch.removeTransaction(txID);
        TxnBatchID completed = batchAccounting.acknowledge(txID);
        if (!completed.isNull()) {
          incompleteBatches.remove(completed);
          if (status == STOP_INITIATED && incompleteBatches.size() == 0) {
            logger.debug("Received ACK for the last Transaction. Moving to STOPPED state.");
            status = STOPPED;
          }
        }
      } else {
        logger.fatal("No batch found for acknowledgement: " + txID + " The batch accounting is " + batchAccounting);
        throw new AssertionError("No batch found for acknowledgement: " + txID);
      }
      lock.notifyAll();
      callbacks = getLockFlushCallbacks(completedLocks);
    }
    fireLockFlushCallbacks(callbacks);
  }

  private TransactionID getCompletedTransactionIDLowWaterMark() {
    synchronized (lock) {
      waitUntilRunning();
      return batchAccounting.getLowWaterMark();
    }
  }

  /*
   * Never fire callbacks while holding lock
   */
  private void fireLockFlushCallbacks(Map callbacks) {
    if (callbacks.isEmpty()) return;
    for (Iterator i = callbacks.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      LockID lid = (LockID) e.getKey();
      LockFlushCallback callback = (LockFlushCallback) e.getValue();
      callback.transactionsForLockFlushed(lid);
    }
  }

  private Map getLockFlushCallbacks(Set completedLocks) {
    Map callbacks = Collections.EMPTY_MAP;
    if (!completedLocks.isEmpty() && !lockFlushCallbacks.isEmpty()) {
      for (Iterator i = completedLocks.iterator(); i.hasNext();) {
        Object lid = i.next();
        Object callback = lockFlushCallbacks.remove(lid);
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
    while (status != RUNNING) {
      try {
        lock.wait();
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
    return batchAccounting;
  }

  void setAckOnExitTimeout(int seconds) {
    this.ackOnExitTimeout = seconds * 1000;
  }

  private class RemoteTransactionManagerTimerTask extends TimerTask {

    private TransactionID currentLWM = TransactionID.NULL_ID;

    public void run() {
      try {
        TransactionID lwm = getCompletedTransactionIDLowWaterMark();
        if (lwm.isNull()) return;
        if (currentLWM.toLong() > lwm.toLong()) { throw new AssertionError("Transaction Low watermark moved down from "
                                                                           + currentLWM + " to " + lwm); }
        currentLWM = lwm;
        CompletedTransactionLowWaterMarkMessage ctm = channel.getCompletedTransactionLowWaterMarkMessageFactory()
            .newCompletedTransactionLowWaterMarkMessage(groupID);
        ctm.initialize(lwm);
        ctm.send();
      } catch (Exception e) {
        logger.error("Error sending Low water mark : ", e);
        throw new AssertionError(e);
      }
    }
  }
}
