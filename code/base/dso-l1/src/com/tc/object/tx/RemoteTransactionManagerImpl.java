/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.State;
import com.tc.util.TCAssertionError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends off committed transactions
 *
 * @author steve
 */
public class RemoteTransactionManagerImpl implements RemoteTransactionManager {

  private static final long                TIMEOUT                 = 30000L;
  private static final int                 MAX_TRANSACTION_SIZE    = 1000;
  private static final int                 MAX_OUTSTANDING_BATCHES = 2;
  private static final State               STARTING                = new State("STARTING");
  private static final State               RUNNING                 = new State("RUNNING");
  private static final State               PAUSED                  = new State("PAUSED");
  private static final State               STOP_INITIATED          = new State("STOP-INITIATED");
  private static final State               STOPPED                 = new State("STOPPED");

  private final TransactionBatchFactory    batchFactory;

  private final Object                     lock                    = new Object();
  private final Map                        incompleteBatches       = new HashMap();
  private final HashMap                    lockFlushCallbacks      = new HashMap();

  private ClientTransactionBatch           currentBatch;
  private final Object                     currentBatchLock        = new Object();
  private int                              outStandingBatches      = 0;
  private final TCLogger                   logger;
  private final TransactionBatchAccounting batchAccounting;
  private final LockAccounting             lockAccounting;

  private State                            status;
  private final SessionManager             sessionManager;

  public RemoteTransactionManagerImpl(TCLogger logger, final TransactionBatchFactory batchFactory,
                                      TransactionBatchAccounting batchAccounting, LockAccounting lockAccounting,
                                      SessionManager sessionManager) {
    this.logger = logger;
    this.batchFactory = batchFactory;
    this.batchAccounting = batchAccounting;
    this.lockAccounting = lockAccounting;
    this.sessionManager = sessionManager;
    this.status = RUNNING;
    currentBatch = createNewBatch();
  }

  public void pause() {
    synchronized (lock) {
      if (isStoppingOrStopped()) return;
      if (this.status == PAUSED) throw new AssertionError("Attempt to pause while already paused.");
      this.status = PAUSED;
    }
  }

  public void starting() {
    synchronized (lock) {
      if (isStoppingOrStopped()) return;
      if (this.status != PAUSED) throw new AssertionError("Attempt to start while not paused.");
      this.status = STARTING;
    }
  }

  public void unpause() {
    synchronized (lock) {
      if (isStoppingOrStopped()) return;
      if (this.status != STARTING) throw new AssertionError("Attempt to unpause while not in starting.");
      this.status = RUNNING;
      lock.notifyAll();
    }
  }

  /**
   * This is for testing only.
   */
  public void clear() {
    synchronized (lock) {
      currentBatch = createNewBatch();
      incompleteBatches.clear();
    }
  }

  /**
   * This is for testing only.
   */
  public int getMaxOutStandingBatches() {
    return MAX_OUTSTANDING_BATCHES;
  }

  public void stop() {
    final long start = System.currentTimeMillis();
    logger.debug("stop() is called on " + System.identityHashCode(this));
    synchronized (lock) {
      this.status = STOP_INITIATED;

      if (!currentBatch.isEmpty()) {
        // Send the betch
        logger.debug("stop(): Current batch has " + currentBatch.numberOfTxns() + " transactions. Sending them");
        sendCurrentBatch();
      }

      int count = 10;
      long t0 = System.currentTimeMillis();
      if (incompleteBatches.size() != 0) {
        try {
          int incompleteBatchesCount = 0;
          while (status != STOPPED && (t0 + TIMEOUT * count) > System.currentTimeMillis()) {
            if (incompleteBatchesCount != incompleteBatches.size()) {
              logger.debug("stop(): incompleteBatches.size() = " + (incompleteBatchesCount = incompleteBatches.size()));
            }
            lock.wait(TIMEOUT);
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
    Collection c;
    synchronized (lock) {
      while ((!(c = lockAccounting.getTransactionsFor(lockID)).isEmpty())) {
        try {
          long waitTime = 15 * 1000;
          long t0 = System.currentTimeMillis();
          lock.wait(waitTime);
          if ((System.currentTimeMillis() - t0) > waitTime) {
            logger.info("Flush for " + lockID + " took longer than: " + waitTime
                        + "ms. # Transactions not yet Acked = " + c.size() + "\n");
          }
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
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
          // Will this scenario comeup in server restart scenario ? It should as we check for greediness in the Lock
          // Manager before making this call
          throw new TCAssertionError("There is already a registered call back on Lock Flush for this lock ID - "
                                     + lockID);
        }
        return false;
      }
    }
  }

  private final SequenceGenerator sequence = new SequenceGenerator(1);

  public void commit(ClientTransaction txn) {
    TransactionID txID = txn.getTransactionID();

    if (!txn.hasChangesOrNotifies()) throw new AssertionError("Attempt to commit an empty transaction.");

    synchronized (currentBatchLock) {
      SequenceID sequenceID = new SequenceID(sequence.getNextSequence());
      txn.setSequenceID(sequenceID);

      if (!txn.isConcurrent()) {
        lockAccounting.add(txID, Arrays.asList(txn.getAllLockIDs()));
      }

      addTransactionToBatch(txn, currentBatch);
    }

    synchronized (lock) {
      waitUntilRunning();

      if (canSendBatch() && !currentBatch.isEmpty()) {
        sendCurrentBatch();
      }
      // We throttle batch after adding to the batch. This will not allow the thread to go forward
      // and create more transactions.
      throttleBatchSize();
    }
  }

  private boolean canSendBatch() {
    return (outStandingBatches < MAX_OUTSTANDING_BATCHES);
  }

  public void resendOutstanding() {
    synchronized (lock) {
      if (status != STARTING && !isStoppingOrStopped()) {
        // formatting
        throw new AssertionError(this + ": Attempt to resend incomplete batches while not starting.  Status=" + status);
      }
      logger.debug("resendOutstanding()...");
      outStandingBatches = 0;
      List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
      if (toSend.size() == 0) {
        if (currentBatch.isEmpty()) {
          logger.debug("resendOutstanding(): current batch is empty."
                       + "  The next transaction commit will cause it to be sent.");
        } else {
          logger.debug("resendOutstanding(): current Batch : " + currentBatch);
          sendCurrentBatch();
        }
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

  public Collection getTransactionSequenceIDs() {
    synchronized (lock) {
      HashSet sequenceIDs = new HashSet();
      if (!isStoppingOrStopped() && (status != STARTING)) {
        throw new AssertionError("Attempt to get current transaction sequence while not starting: " + status);
      } else {
        // Add list of SequenceIDs that are going to be resent
        List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
        for (Iterator i = toSend.iterator(); i.hasNext();) {
          TxnBatchID id = (TxnBatchID) i.next();
          ClientTransactionBatch batch = (ClientTransactionBatch) incompleteBatches.get(id);
          if (batch == null) throw new AssertionError("Unknown batch: " + id);
          batch.addTransactionSequenceIDsTo(sequenceIDs);
        }
        // Add Last next
        SequenceID currentBatchMinSeq = currentBatch.getMinTransactionSequence();
        if (SequenceID.NULL_ID.equals(currentBatchMinSeq)) {
          SequenceID currentSequenceID = new SequenceID(sequence.getCurrentSequence());
          sequenceIDs.add(currentSequenceID.next());
        } else {
          sequenceIDs.add(currentBatchMinSeq);
        }
      }
      return sequenceIDs;
    }
  }

  public Collection getResentTransactionIDs() {
    synchronized (lock) {
      HashSet txIDs = new HashSet();
      if (!isStoppingOrStopped() && (status != STARTING)) {
        throw new AssertionError("Attempt to get resent transaction IDs while not starting: " + status);
      } else {
        // Add list of TransactionIDs that are going to be resent
        List toSend = batchAccounting.addIncompleteBatchIDsTo(new ArrayList());
        for (Iterator i = toSend.iterator(); i.hasNext();) {
          TxnBatchID id = (TxnBatchID) i.next();
          ClientTransactionBatch batch = (ClientTransactionBatch) incompleteBatches.get(id);
          if (batch == null) throw new AssertionError("Unknown batch: " + id);
          batch.addTransactionIDsTo(txIDs);
        }
      }
      return txIDs;
    }
  }

  private boolean isStoppingOrStopped() {
    return status == STOP_INITIATED || status == STOPPED;
  }

  private void addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    batch.addTransaction(txn);
  }

  private void sendCurrentBatch() {
    synchronized (currentBatchLock) {
      sendBatch(currentBatch, true);
      currentBatch = createNewBatch();
    }
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
        batchToSend.addAcknowledgedTransactionIDs(batchAccounting.addCompletedTransactionIDsTo(new HashSet()));
        batchAccounting.clearCompletedTransactionIds();
      }
      batchToSend.send();
      outStandingBatches++;
    }
  }

  private ClientTransactionBatch createNewBatch() {
    return batchFactory.nextBatch();
  }

  // If there are commits struck here and the application terminates, chances are these
  // transactions will not be sent to the server. Such a behavior in the application indicates
  // a bug in the client application.
  private void throttleBatchSize() {
    while (currentBatch.numberOfTxns() > MAX_TRANSACTION_SIZE || status != RUNNING) {
      try {
        lock.wait();
      } catch (InterruptedException ie) {
        throw new AssertionError();
      }
    }
  }

  public void receivedBatchAcknowledgement(TxnBatchID txnBatchID) {
    synchronized (lock) {
      if (status == STOP_INITIATED) {
        logger.warn(status + " : Received ACK for a batch. The Current Batch size = " + currentBatch.numberOfTxns());
        lock.notifyAll();
        return;
      }

      waitUntilRunning();
      outStandingBatches--;
      if (canSendBatch() && !currentBatch.isEmpty()) {
        sendCurrentBatch();
      }
      lock.notifyAll();
    }
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID txID) {
    Set completedLocks;

    synchronized (lock) {
      // waitUntilRunning();
      if (!sessionManager.isCurrentSession(sessionID)) {
        logger.warn("Ignoring Transaction ACK for " + txID + " from previous session = " + sessionID);
        return;
      }

      completedLocks = lockAccounting.acknowledge(txID);

      TxnBatchID container = batchAccounting.getBatchByTransactionID(txID);
      if (!container.isNull()) {
        ClientTransactionBatch containingBatch = (ClientTransactionBatch) incompleteBatches.get(container);
        containingBatch.removeTransaction(txID);
        TxnBatchID completed = batchAccounting.acknowledge(txID);
        if (!completed.isNull()) {
          incompleteBatches.remove(completed);
          if (status == STOP_INITIATED && incompleteBatches.size() == 0) {
            logger.debug("Received ACK for the last Transaction. Moving to STOPPED state. The Current Batch size = "
                         + currentBatch.numberOfTxns());
            status = STOPPED;
          }
        }
      } else {
        logger.fatal("No batch found for acknowledgement: " + txID + " The batch accounting is " + batchAccounting);
        throw new AssertionError("No batch found for acknowledgement: " + txID);
      }
      lock.notifyAll();

      // doLockFlushCallbacks() is moved inside the synchronized() block due to a race condition
      // between flush and recallCommit.
      doLockFlushCallbacks(completedLocks);
    }
  }

  private void doLockFlushCallbacks(Set completedLocks) {
    // Do callback
    if (!completedLocks.isEmpty() && !lockFlushCallbacks.isEmpty()) {
      for (Iterator i = completedLocks.iterator(); i.hasNext();) {
        LockID lid = (LockID) i.next();
        LockFlushCallback callback = (LockFlushCallback) lockFlushCallbacks.remove(lid);
        if (callback != null) {
          callback.transactionsForLockFlushed(lid);
        }
      }
    }
  }

  private void waitUntilRunning() {
    while (status != RUNNING)
      try {
        lock.wait();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
  }

  // This method exists so that both these (resending and unpausing) should happen in
  // atomically or else there exists a race condition.
  public void resendOutstandingAndUnpause() {
    synchronized (lock) {
      resendOutstanding();
      unpause();
    }
  }
}
