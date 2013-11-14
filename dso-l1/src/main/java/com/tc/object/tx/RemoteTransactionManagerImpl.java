/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.text.PrettyPrinter;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.Assert;
import com.tc.util.SequenceID;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends off committed transactions
 */
public class RemoteTransactionManagerImpl implements RemoteTransactionManager {

  private static final long                              FLUSH_WAIT_INTERVAL         = 15 * 1000;

  private static final int                               MAX_OUTSTANDING_BATCHES     = TCPropertiesImpl
                                                                                         .getProperties()
                                                                                         .getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE);
  private static final long                              COMPLETED_ACK_FLUSH_TIMEOUT = TCPropertiesImpl
                                                                                         .getProperties()
                                                                                         .getLong(TCPropertiesConsts.L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT);

  private static final State                             RUNNING                     = new State("RUNNING");
  private static final State                             PAUSED                      = new State("PAUSED");
  private static final State                             STARTING                    = new State("STARTING");
  private static final State                             REJOIN_IN_PROGRESS          = new State("REJOIN_IN_PROGRESS");
  private static final State                             STOP_INITIATED              = new State("STOP-INITIATED");
  private static final State                             STOPPED                     = new State("STOPPED");

  private final Object                                   lock                        = new Object();
  private final HashMap<LockID, List<LockFlushCallback>> lockFlushCallbacks          = new HashMap<LockID, List<LockFlushCallback>>();

  private final BatchManager                             batchManager;
  private final AtomicBoolean                            sending                     = new AtomicBoolean();
  private TransactionBatchAccounting                     batchAccounting             = new TransactionBatchAccounting();
  private final LockAccounting                           lockAccounting;
  private final TCLogger                                 logger;
  private final long                                     ackOnExitTimeout;
  private volatile State                                 status;
  private final SessionManager                           sessionManager;
  private final TransactionSequencer                     sequencer;
  private final DSOClientMessageChannel                  channel;

  private final GroupID                                  groupID;

  private volatile boolean                               isShutdown                  = false;
  private volatile boolean                               isThrottled                 = false;
  private final AbortableOperationManager                abortableOperationManager;

  private final Timer                                    flusherTimer;
  private final RemoteTransactionManagerTask             remoteTxManagerRunnable;
  // this lock protects the state change during rejoin and addition of data to the sequencer.
  private final ReadWriteLock                            rejoinCleanupLock           = new ReentrantReadWriteLock();
  private volatile boolean                               immediateShutdownRequested  = false;

  // for testing
  RemoteTransactionManagerImpl(BatchManager batchManager, TransactionBatchAccounting batchAccounting,
                               LockAccounting lockAccounting, TCLogger logger, long ackOnExitTimeout, State status,
                               SessionManager sessionManager, TransactionSequencer sequencer,
                               DSOClientMessageChannel channel, GroupID groupID, boolean isShutdown,
                               AbortableOperationManager abortableOperationManager, Timer flusherTimer,
                               RemoteTransactionManagerTask remoteTxManagerRunnable) {
    this.batchManager = batchManager;
    this.batchAccounting = batchAccounting;
    this.lockAccounting = lockAccounting;
    this.logger = logger;
    this.ackOnExitTimeout = ackOnExitTimeout;
    this.status = status;
    this.sessionManager = sessionManager;
    this.sequencer = sequencer;
    this.channel = channel;
    this.groupID = groupID;
    this.isShutdown = isShutdown;
    this.abortableOperationManager = abortableOperationManager;
    this.flusherTimer = flusherTimer;
    this.remoteTxManagerRunnable = remoteTxManagerRunnable;
  }

  public RemoteTransactionManagerImpl(final GroupID groupID, final TCLogger logger,
                                      final TransactionBatchFactory batchFactory,
                                      final TransactionIDGenerator transactionIDGenerator,
                                      final SessionManager sessionManager, final DSOClientMessageChannel channel,
                                      final SampledRateCounter transactionSizeCounter,
                                      final SampledRateCounter transactionsPerBatchCounter,
                                      final long ackOnExitTimeoutMs,
                                      final AbortableOperationManager abortableOperationManager,
                                      final TaskRunner taskRunner) {
    this.groupID = groupID;
    this.logger = logger;
    this.sessionManager = sessionManager;
    this.channel = channel;
    this.status = RUNNING;
    this.ackOnExitTimeout = ackOnExitTimeoutMs;
    this.lockAccounting = new LockAccounting(abortableOperationManager, this);
    this.sequencer = new TransactionSequencer(groupID, transactionIDGenerator, batchFactory, this.lockAccounting,
                                              transactionSizeCounter, transactionsPerBatchCounter,
                                              abortableOperationManager, this);
    this.remoteTxManagerRunnable = new RemoteTransactionManagerTask();
    this.flusherTimer = taskRunner.newTimer("RemoteTransactionManager Flusher");
    this.flusherTimer.scheduleWithFixedDelay(this.remoteTxManagerRunnable, COMPLETED_ACK_FLUSH_TIMEOUT,
                                             COMPLETED_ACK_FLUSH_TIMEOUT, TimeUnit.MILLISECONDS);
    this.batchManager = new BatchManager();
    this.abortableOperationManager = abortableOperationManager;
    batchManager.start();
  }

  @Override
  public void preCleanup() {
    // do not take rejoinCleanUpLock as it wont cause existing threads in throttling to kick out..
    synchronized (this.lock) {
      checkAndSetstate();
      sequencer.cleanup();
    }
  }

  @Override
  public void cleanup() {
    synchronized (this.lock) {
      // this is outside the rejoinCleanUpLock in order to kick out all the threads that are throttling at present..
      checkAndSetstate();
    }
    rejoinCleanupLock.writeLock().lock();
    try {
      synchronized (this.lock) {
        lockFlushCallbacks.clear();
        batchManager.clear();
        batchAccounting = new TransactionBatchAccounting();
        sequencer.cleanup();
      }
    } finally {
      rejoinCleanupLock.writeLock().unlock();
    }
  }

  private void checkAndSetstate() {
    throwExceptionIfNecessary(true);
    status = REJOIN_IN_PROGRESS;
    this.lock.notifyAll();
  }

  private void throwExceptionIfNecessary(boolean throwExp) {
    if (status != PAUSED && status != REJOIN_IN_PROGRESS) {
      String message = "cleanup unexpected state: expected " + PAUSED + " OR " + REJOIN_IN_PROGRESS + " but found "
                       + status;
      if (throwExp) {
        throw new IllegalStateException(message);
      } else {
        logger.warn(message);
      }
    }
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    lockAccounting.shutdown();
    isShutdown = true;
    flusherTimer.cancel();
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  @Override
  public void pause(final NodeID remote, final int disconnected) {
    if (this.isShutdown) { return; }
    this.remoteTxManagerRunnable.reset();
    if (isStoppingOrStopped()) { return; }
    synchronized (this.lock) {
      if (this.status == PAUSED) { throw new AssertionError("Attempt to pause while already paused state."); }
      this.status = PAUSED;
    }
    boolean isInterrupted = false;
    while (true) {
      try {
        batchManager.stop();
        break;
      } catch (InterruptedException ie) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  @Override
  public void unpause(final NodeID remote, final int disconnected) {
    if (this.isShutdown) { return; }
    if (isStoppingOrStopped()) { return; }

    synchronized (this.lock) {
      if (this.status == RUNNING) { throw new AssertionError("Attempt to unpause while in running state."); }
    }

    batchManager.start();
    resendOutstanding();

    synchronized (this.lock) {
      if (this.status == RUNNING) { throw new AssertionError("Attempt to unpause while in running state."); }
      this.status = RUNNING;
      this.lock.notifyAll();
    }
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    if (this.isShutdown) { return; }
    synchronized (this.lock) {
      State current = this.status;
      if (!(current == PAUSED || current == REJOIN_IN_PROGRESS)) { throw new AssertionError(
                                                                                            "At from "
                                                                                                + remoteNode
                                                                                                + " to "
                                                                                                + thisNode
                                                                                                + " . "
                                                                                                + "Attempting to handshake while "
                                                                                                + current); }
      this.status = STARTING;
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
      this.batchManager.clear();
    }
  }

  int getMaxOutStandingBatches() {
    return MAX_OUTSTANDING_BATCHES;
  }

  int getMaxQueuedBatches() {
    return MAX_OUTSTANDING_BATCHES + sequencer.getMaxPendingSize();
  }

  @Override
  public void stopProcessing() {
    this.sequencer.shutdown();
    this.channel.close();
  }

  @Override
  public void requestImmediateShutdown() {
    this.immediateShutdownRequested = true;
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  @Override
  public void stop() {
    final long start = System.currentTimeMillis();
    this.logger.debug("stop() is called on " + System.identityHashCode(this));
    synchronized (this.lock) {
      if (this.status == RUNNING) {
        this.status = STOP_INITIATED;
      }
    }

    sendBatches(true, "stop()");

    final long pollInteval = (this.ackOnExitTimeout > 0) ? (this.ackOnExitTimeout / 10) : (30 * 1000);
    final long t0 = System.currentTimeMillis();
    if (!batchManager.isEmpty()) {
      try {
        int incompleteBatchesCount = 0;
        final LossyTCLogger lossyLogger = new LossyTCLogger(this.logger, 5, LossyTCLoggerType.COUNT_BASED);
        State currentState = STOP_INITIATED;

        while ((currentState != STOPPED) && !immediateShutdownRequested
               && ((this.ackOnExitTimeout <= 0) || (t0 + this.ackOnExitTimeout) > System.currentTimeMillis())) {
          if (incompleteBatchesCount != batchManager.size()) {
            lossyLogger.info("stop(): incompleteBatches.size() = " + (incompleteBatchesCount = batchManager.size()));
          }
          synchronized (this.lock) {
            this.lock.wait(pollInteval);
          }
          currentState = this.status;
        }
      } catch (final InterruptedException e) {
        this.logger.warn("stop(): Interrupted " + e);
        Thread.currentThread().interrupt();
      }
      synchronized (this.lock) {
        if (this.status != STOPPED) {
          this.logger.error("stop() : There are still UNACKed Transactions! incompleteBatches.size() = "
                            + this.batchManager.size());
        }
      }
    }

    stopIfStopping();
    try {
      batchManager.stop();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    this.logger.info("stop(): took " + (System.currentTimeMillis() - start) + " millis to complete");
  }

  @Override
  public void flush(final LockID lockID) throws AbortedOperationException {
    final long start = System.currentTimeMillis();
    long lastPrinted = 0;
    boolean isInterrupted = false;
    try {
      Collection c;
      synchronized (this.lock) {
        while ((!(c = this.lockAccounting.getTransactionsFor(lockID)).isEmpty())) {
          try {
            this.lock.wait(FLUSH_WAIT_INTERVAL);
            final long now = System.currentTimeMillis();
            if ((now - start) > FLUSH_WAIT_INTERVAL && (now - lastPrinted) > FLUSH_WAIT_INTERVAL / 3) {
              this.logger.info("Flush for " + lockID + " took longer than: " + (FLUSH_WAIT_INTERVAL / 1000)
                               + " sec. Took : " + (now - start) + " ms. # Transactions not yet Acked = "
                               + (c.size() + (c.size() < 50 ? (". " + c) : "")) + "\n");
              lastPrinted = now;
            }
          } catch (final InterruptedException e) {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
            isInterrupted = true;
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  @Override
  public void waitForServerToReceiveTxnsForThisLock(final LockID lockId) throws AbortedOperationException {
    // wait for transactions to get acked here from the server
    final long start = System.currentTimeMillis();
    long lastPrinted = 0;
    boolean isInterrupted = false;
    try {
      synchronized (this.lock) {
        while (!this.lockAccounting.areTransactionsReceivedForThisLockID(lockId)) {
          try {
            this.lock.wait(FLUSH_WAIT_INTERVAL);
            final long now = System.currentTimeMillis();
            if ((now - start) > FLUSH_WAIT_INTERVAL && (now - lastPrinted) > FLUSH_WAIT_INTERVAL / 3) {
              this.logger.info("Sync Write for " + lockId + " took longer than: " + (FLUSH_WAIT_INTERVAL / 1000)
                               + " sec. Took : " + (now - start) + " ms.\n");
              lastPrinted = now;
            }
          } catch (final InterruptedException e) {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
            isInterrupted = true;
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  /**
   * This method will be called when the server receives a batch. This should ideally be called only when a batch
   * contains a sync write transaction.
   */
  @Override
  public void batchReceived(final TxnBatchID batchId, final Set<TransactionID> syncTxnSet, final NodeID nid) {
    // This batch id was received by the server
    // so notify the locks waiting for this transaction

    synchronized (this.lock) {
      this.lockAccounting.transactionRecvdByServer(syncTxnSet);
      this.lock.notifyAll();
    }
  }

  /* This does not block unlike flush() */
  @Override
  public boolean asyncFlush(final LockID lockID, final LockFlushCallback callback) {
    synchronized (this.lock) {

      if ((this.lockAccounting.getTransactionsFor(lockID)).isEmpty()) {
        // All transactions are flushed !
        return true;
      } else {
        // register for call back
        if (callback != null) {
          List<LockFlushCallback> lockFlushCallbacksList = this.lockFlushCallbacks.get(lockID);
          if (lockFlushCallbacksList == null) {
            lockFlushCallbacksList = new ArrayList<LockFlushCallback>();
            this.lockFlushCallbacks.put(lockID, lockFlushCallbacksList);
          }
          lockFlushCallbacksList.add(callback);
        }
        return false;
      }
    }
  }

  @Override
  public void commit(final ClientTransaction txn) throws AbortedOperationException {
    rejoinCleanupLock.readLock().lock();
    try {
      if (isRejoinInProgress()) { throw new PlatformRejoinException(); }
      throttleIfNecessary();
      commitWithoutThrottling(txn);
    } finally {
      rejoinCleanupLock.readLock().unlock();
    }
  }

  void throttleIfNecessary() throws AbortedOperationException {
    this.sequencer.throttleIfNecesary();
  }

  void commitWithoutThrottling(final ClientTransaction txn) {
    if (!txn.hasChangesOrNotifies() && txn.getDmiDescriptors().isEmpty() && txn.getNewRoots().isEmpty()) {
      //
      throw new AssertionError("Attempt to commit an empty transaction.");
    }
    if (!txn.getTransactionID().isNull()) { throw new AssertionError(
                                                                     "Transaction already committed as TransactionID is already assigned"); }
    final long start = System.currentTimeMillis();

    this.sequencer.addTransaction(txn);

    final long diff = System.currentTimeMillis() - start;
    if (diff > 1000) {
      this.logger.info(txn.getTransactionID() + " : Took more than 1000ms to add to sequencer  : " + diff + " ms");
    }

    sendBatches(!isRunning(true));
  }

  private void stopIfStopping() {
    synchronized (this.lock) {
      if (this.status == STOP_INITIATED) {
        this.logger.debug("Received ACK for the last Transaction. Moving to STOPPED state.");
        this.status = STOPPED;
      }
    }
  }

  private boolean isRunning(boolean abortable) {
    if (isStoppingOrStopped()) {
      logger.debug("commit() : Stop initiated.");
      return false;
    } else {
      try {
        if (abortable) {
          waitUntilRunningAbortable();
        } else {
          waitUntilRunning();
        }
      } catch (AbortedOperationException e) {
        logger.debug("Ignoring Aborted Operation Exception since the transaction is already written to the sequencer.");
      }
    }
    return true;
  }

  private void sendBatches(final boolean ignoreMax) {
    if (ignoreMax || sending.compareAndSet(false, true)) {
      sendBatches(ignoreMax, null);
      sending.set(false);
    }
  }

  private void sendBatches(final boolean ignoreMax, final String message) {
    ClientTransactionBatch batch = batchManager.sendNextBatch(ignoreMax);
    while (batch != null) {
      if (message != null && logger.isDebugEnabled()) {
        this.logger.debug(message + " : Sending batch containing " + batch.numberOfTxnsBeforeFolding() + " txns");
      }
      batch = batchManager.sendNextBatch(ignoreMax);
    }
  }

  void resendOutstanding() {
    final List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
    if (toSend.isEmpty()) {
      sendBatches(false, " resendOutstanding()");
    } else {
      batchManager.resendList(toSend);
    }
    batchManager.waitForEmpty();
  }

  List getTransactionSequenceIDs() {
    final ArrayList sequenceIDs = new ArrayList();
    final List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
    if (!toSend.isEmpty()) {
      for (Object obj : toSend) {
        final TxnBatchID id = (TxnBatchID) obj;
        final ClientTransactionBatch batch = batchManager.getBatch(id);
        if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
        batch.addTransactionSequenceIDsTo(sequenceIDs);
      }
    }
    // Add Last next
    final SequenceID currentBatchMinSeq = this.sequencer.getNextSequenceID();
    Assert.assertFalse(SequenceID.NULL_ID.equals(currentBatchMinSeq));
    sequenceIDs.add(currentBatchMinSeq);
    return sequenceIDs;
  }

  List getResentTransactionIDs() {
    final ArrayList txIDs = new ArrayList();
    final List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
    if (!toSend.isEmpty()) {
      for (Object obj : toSend) {
        final TxnBatchID id = (TxnBatchID) obj;
        final ClientTransactionBatch batch = batchManager.getBatch(id);
        if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
        batch.addTransactionIDsTo(txIDs);
      }
    }
    return txIDs;
  }

  private boolean isStoppingOrStopped() {
    synchronized (this.lock) {
      return this.status == STOP_INITIATED || this.status == STOPPED;
    }
  }

  boolean isRejoinInProgress() {
    // called from transaction sequencer do not take synchronize status is volatile.
    return this.status == REJOIN_IN_PROGRESS;
  }

  // XXX:: Currently server always sends NULL BatchID
  @Override
  public void receivedBatchAcknowledgement(final TxnBatchID txnBatchID, final NodeID remoteNode) {
    try {
      if (!isRunning(false)) {
        this.logger.warn(this.status + " : Received ACK for batch = " + txnBatchID);
        return;
      }
      if (this.logger.isDebugEnabled()) {
        this.logger.debug(batchManager.toString());
      }
      batchManager.batchAcknowledged();
      sendBatches(false);
    } finally {
      synchronized (this.lock) {
        lock.notifyAll();
      }
    }
  }

  @Override
  public TransactionBuffer receivedAcknowledgement(final SessionID sessionID, final TransactionID txID,
                                                   final NodeID remoteNode) {
    TransactionBuffer tb = null;
    Map callbacks = null;
    if (!this.sessionManager.isCurrentSession(remoteNode, sessionID)) {
      this.logger.warn("Ignoring Transaction ACK for " + txID + " from previous session = " + sessionID);
      return tb;
    }

    final TxnBatchID container = this.batchAccounting.getBatchByTransactionID(txID);
    if (!container.isNull()) {
      final ClientTransactionBatch containingBatch = batchManager.getBatch(container);
      tb = containingBatch.removeTransaction(txID);
      callBackTxnCompleteListeners(tb.getFoldedTransactionID(), tb.getTransactionCompleteListeners());
      final TxnBatchID completed = this.batchAccounting.acknowledge(txID);
      if (!completed.isNull()) {
        batchManager.removeBatch(completed);
        if (isStoppingOrStopped() && batchManager.isEmpty()) {
          stopIfStopping();
        } else {
          sendBatches(false);
        }
      }
    } else {
      this.logger.fatal("No batch found for acknowledgement: " + txID + " The batch accounting is "
                        + this.batchAccounting);
      throw new AssertionError("No batch found for acknowledgement: " + txID);
    }

    synchronized (this.lock) {
      final Set completedLocks = this.lockAccounting.acknowledge(txID);
      callbacks = getLockFlushCallbacks(completedLocks);
      this.lock.notifyAll();
    }

    fireLockFlushCallbacks(callbacks);
    return tb;
  }

  private void callBackTxnCompleteListeners(TransactionID txnID,
                                            List<TransactionCompleteListener> transactionCompleteListeners) {
    if (transactionCompleteListeners.isEmpty()) return;
    try {
      for (TransactionCompleteListener l : transactionCompleteListeners) {
        l.transactionComplete(txnID);
      }
    } catch (TCNotRunningException e) {
      if (!isShutdown()) { throw e; }
    }
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    this.lockAccounting.waitAllCurrentTxnCompleted();
  }

  private TransactionID getCompletedTransactionIDLowWaterMark() {
    waitUntilRunning();
    return this.batchAccounting.getLowWaterMark();
  }

  /*
   * Never fire callbacks while holding lock
   */
  private void fireLockFlushCallbacks(final Map<LockID, List<LockFlushCallback>> callbacks) {
    if (callbacks.isEmpty()) { return; }
    for (Entry<LockID, List<LockFlushCallback>> element : callbacks.entrySet()) {
      final LockID lid = element.getKey();
      final List<LockFlushCallback> callbacksList = element.getValue();
      for (LockFlushCallback callback : callbacksList) {
        callback.transactionsForLockFlushed(lid);
      }
    }
  }

  private Map<LockID, List<LockFlushCallback>> getLockFlushCallbacks(final Set completedLocks) {
    Map<LockID, List<LockFlushCallback>> callbacks = Collections.emptyMap();
    if (!completedLocks.isEmpty() && !this.lockFlushCallbacks.isEmpty()) {
      for (final LockID lid : (Iterable<LockID>) completedLocks) {
        final List<LockFlushCallback> lockFlushCallbacksList = this.lockFlushCallbacks.remove(lid);
        if (lockFlushCallbacksList != null) {
          if (callbacks == Collections.EMPTY_MAP) {
            callbacks = new HashMap<LockID, List<LockFlushCallback>>();
          }
          callbacks.put(lid, lockFlushCallbacksList);
        }
      }
    }
    return callbacks;
  }

  /**
   * waits until the Transaction manager is in running state.
   */
  private void waitUntilRunning() {
    boolean isInterrupted = false;
    synchronized (this.lock) {
      try {
        if (this.status == STOPPED || this.status == STOP_INITIATED) { return; }
        while (this.status != RUNNING) {
          if (isShutdown) { throw new TCNotRunningException(); }
          if (this.status == REJOIN_IN_PROGRESS) { throw new PlatformRejoinException(); }
          try {
            this.lock.wait();
          } catch (final InterruptedException e) {
            isInterrupted = true;
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
    }
  }

  /**
   * waits until the Transaction manager is in running state.
   * 
   * @throws AbortedOperationException If the Operation is aborted.
   */
  private void waitUntilRunningAbortable() throws AbortedOperationException {
    boolean isInterrupted = false;
    synchronized (this.lock) {
      try {
        while (this.status != RUNNING) {
          if (isShutdown) { throw new TCNotRunningException(); }
          if (this.status == REJOIN_IN_PROGRESS) { throw new PlatformRejoinException(); }
          try {
            this.lock.wait();
          } catch (final InterruptedException e) {
            isInterrupted = true;
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
    }
  }

  /*
   * For Tests
   */
  TransactionBatchAccounting getBatchAccounting() {
    return this.batchAccounting;
  }

  class BatchManager {
    /* make sure there is enough room for batches if max is ignored */
    private final ArrayBlockingQueue<ClientTransactionBatch> sendList           = new ArrayBlockingQueue<ClientTransactionBatch>(
                                                                                                                                 sequencer
                                                                                                                                     .getMaxPendingSize()
                                                                                                                                     + getMaxOutStandingBatches());
    private Thread                                           agent;
    private volatile boolean                                 stopping           = false;
    private boolean                                          empty              = true;
    private SequenceID                                       lastsid;
    private int                                              outStandingBatches = 0;
    private final Map<TxnBatchID, ClientTransactionBatch>    incompleteBatches  = new HashMap<TxnBatchID, ClientTransactionBatch>();

    public synchronized void clear() {
      lastsid = null;
      outStandingBatches = 0;
      empty = true;
      incompleteBatches.clear();
      this.notify();
    }

    public synchronized void waitForEmpty() {
      while (!empty) {
        try {
          this.wait();
        } catch (InterruptedException ie) {
          //
        }
      }
    }

    public synchronized boolean setEmpty(boolean empty) {
      try {
        if (this.empty != empty) {
          this.notify();
        }
      } finally {
        this.empty = empty;
      }
      return empty;
    }

    public final void start() {
      if (agent != null) { throw new AssertionError(); }
      agent = new Thread(new Runnable() {

        @Override
        public void run() {
          while (true) {
            try {
              ClientTransactionBatch next = sendList.poll(2, TimeUnit.SECONDS);
              if (next != null) {
                next.send();
              } else if (setEmpty(sendList.isEmpty()) && stopping) {
                return;
              } else {
                sendBatches(false);
              }
            } catch (InterruptedException ie) {
              //
            }
          }
        }
      }, "Batch dispatch thread");
      stopping = false;
      agent.setDaemon(true);
      agent.start();
    }

    public void stop() throws InterruptedException {
      if (agent != null) {
        stopping = true;
        agent.join();
        stopping = false;
      }
      agent = null;
    }

    synchronized ClientTransactionBatch sendNextBatch(boolean ignoreMax) {
      int maxOutstanding = (isThrottled) ? 1 : MAX_OUTSTANDING_BATCHES;
      if (ignoreMax
          || (this.outStandingBatches < maxOutstanding && incompleteBatches.size() < MAX_OUTSTANDING_BATCHES * 2)) {
        ClientTransactionBatch batch = sequencer.getNextBatch();
        if (batch != null) {
          if (batch.numberOfTxnsBeforeFolding() == 0) { throw new AssertionError("no transactions"); }
          TxnBatchID bid = batch.getTransactionBatchID();
          batchAccounting.addBatch(bid, markBatchOutstanding(bid, batch));
          addToSendList(batch);
        }
        return batch;
      }
      return null;
    }

    synchronized ClientTransactionBatch getBatch(TxnBatchID id) {
      return incompleteBatches.get(id);
    }

    private synchronized void resendList(List<TxnBatchID> toSend) {
      // sendList.clear();
      lastsid = null;
      for (TxnBatchID id : toSend) {
        final ClientTransactionBatch batch = incompleteBatches.get(id);
        if (batch == null) { throw new AssertionError("Unknown batch: " + id); }
        logger.debug("Resending outstanding batch: " + id + ", " + batch.addTransactionIDsTo(new LinkedHashSet()));
        addToSendList(batch);
      }
    }

    private void addToSendList(ClientTransactionBatch batch) {
      Collection<SequenceID> sids = batch.addTransactionSequenceIDsTo(new ArrayList<SequenceID>());
      for (SequenceID sid : sids) {
        if (lastsid != null && !lastsid.next().equals(sid)) {
          logger.info("skipping some sequence ids.  This must be resend last:" + lastsid + " next:" + sid);
        }
        lastsid = sid;
      }
      setEmpty(false);
      sendList.add(batch);
    }

    private Collection markBatchOutstanding(TxnBatchID bid, final ClientTransactionBatch batchToSend) {
      if (incompleteBatches.put(bid, batchToSend) != null) {
        // formatting
        throw new AssertionError("Batch has already been sent!");
      }
      outStandingBatches++;
      return batchToSend.addTransactionIDsTo(new HashSet());
    }

    private synchronized void batchAcknowledged() {
      outStandingBatches--;
    }

    synchronized ClientTransactionBatch removeBatch(TxnBatchID id) {
      return incompleteBatches.remove(id);
    }

    synchronized boolean isEmpty() {
      return incompleteBatches.isEmpty();
    }

    synchronized int size() {
      return incompleteBatches.size();
    }

    @Override
    public synchronized String toString() {
      return "incomplete:" + incompleteBatches.size();
    }
  }

  private class RemoteTransactionManagerTask implements Runnable {

    private volatile TransactionID currentLWM = TransactionID.NULL_ID;

    @Override
    public void run() {
      synchronized (RemoteTransactionManagerImpl.this.lock) {
        if (status != RUNNING) {
          RemoteTransactionManagerImpl.this.logger.info("Ignoring RemoteTransactionManagerTask because status "
                                                        + status);
          return;
        }
      }
      try {
        final TransactionID lwm = getCompletedTransactionIDLowWaterMark();
        if (lwm.isNull()) { return; }
        if (this.currentLWM.toLong() > lwm.toLong()) { throw new AssertionError(
                                                                                "Transaction Low watermark moved down from "
                                                                                    + this.currentLWM + " to " + lwm); }
        if (this.currentLWM.toLong() == lwm.toLong()) { return; }
        this.currentLWM = lwm;
        final CompletedTransactionLowWaterMarkMessage ctm = RemoteTransactionManagerImpl.this.channel
            .getCompletedTransactionLowWaterMarkMessageFactory()
            .newCompletedTransactionLowWaterMarkMessage(RemoteTransactionManagerImpl.this.groupID);
        ctm.initialize(lwm);
        ctm.send();
      } catch (final TCNotRunningException e) {
        RemoteTransactionManagerImpl.this.logger.info("Ignoring TCNotRunningException while sending Low water mark : ");
      } catch (final PlatformRejoinException e) {
        RemoteTransactionManagerImpl.this.logger.info("Ignoring " + e.getClass().getSimpleName()
                                                      + " while sending Low water mark : ");
      } catch (final Exception e) {
        RemoteTransactionManagerImpl.this.logger.error("Error sending Low water mark : ", e);
        throw new AssertionError(e);
      }
    }

    public void reset() {
      this.currentLWM = TransactionID.NULL_ID;
    }
  }

  @Override
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    synchronized (this.lock) {
      out.indent().print("incompleteBatches count: ").print(this.batchManager.size()).flush();
      out.indent().print("batchAccounting: ").print(this.batchAccounting).flush();
      out.indent().print("lockAccounting: ").print(this.lockAccounting).flush();
    }
    return out;

  }

  // for testing
  public boolean isShutdown() {
    return this.isShutdown;
  }

  @Override
  public void throttleProcessing(boolean yes) {
    this.isThrottled = yes;
  }

}
