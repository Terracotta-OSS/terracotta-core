/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;

public class TransactionSequencer {

  private static final TCLogger         logger         = TCLogging.getLogger(TransactionSequencer.class);

  private static final boolean          LOGGING_ENABLED;
  private static final int              MAX_BYTE_SIZE_FOR_BATCH;
  private static final int              MAX_PENDING_BATCHES;
  private static final long             MAX_SLEEP_TIME_BEFORE_HALT;

  static {
    // Set the values from the properties here.
    LOGGING_ENABLED = TCPropertiesImpl.getProperties().getBoolean("l1.transactionmanager.logging.enabled");
    MAX_BYTE_SIZE_FOR_BATCH = TCPropertiesImpl.getProperties().getInt("l1.transactionmanager.maxBatchSizeInKiloBytes") * 1024;
    MAX_PENDING_BATCHES = TCPropertiesImpl.getProperties().getInt("l1.transactionmanager.maxPendingBatches");
    MAX_SLEEP_TIME_BEFORE_HALT = TCPropertiesImpl.getProperties().getLong("l1.transactionmanager.maxSleepTimeBeforeHalt");
  }

  private final SequenceGenerator       sequence       = new SequenceGenerator(1);
  private final TransactionBatchFactory batchFactory;
  private final BoundedLinkedQueue      pendingBatches = new BoundedLinkedQueue(MAX_PENDING_BATCHES);

  private ClientTransactionBatch        currentBatch;
  private int                           pending_size   = 0;

  private int                           slowDownStartsAt;
  private double                        sleepTimeIncrements;
  private int                           txnsPerBatch   = 0;

  public TransactionSequencer(TransactionBatchFactory batchFactory) {
    this.batchFactory = batchFactory;
    currentBatch = createNewBatch();
    this.slowDownStartsAt = (int) (MAX_PENDING_BATCHES * 0.66);
    this.sleepTimeIncrements = MAX_SLEEP_TIME_BEFORE_HALT / (MAX_PENDING_BATCHES - slowDownStartsAt);
    if (LOGGING_ENABLED) log_settings();
  }

  private void log_settings() {
    logger.info("Max Byte Size for Batches = " + MAX_BYTE_SIZE_FOR_BATCH + " Max Pending Batches = "
                + MAX_PENDING_BATCHES);
    logger.info("Max Sleep time = " + MAX_SLEEP_TIME_BEFORE_HALT + " Slow down starts at = " + slowDownStartsAt
                + " sleep time increments = " + sleepTimeIncrements);
  }

  private ClientTransactionBatch createNewBatch() {
    return batchFactory.nextBatch();
  }

  private void addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    batch.addTransaction(txn);
  }

  /**
   * XXX::Note : There is automatic throttling built in by adding to a BoundedLinkedQueue from within a synch block
   */
  public synchronized void addTransaction(ClientTransaction txn) {
    SequenceID sequenceID = new SequenceID(sequence.getNextSequence());
    txn.setSequenceID(sequenceID);
    txnsPerBatch++;
    addTransactionToBatch(txn, currentBatch);
    if (currentBatch.byteSize() > MAX_BYTE_SIZE_FOR_BATCH) {
      put(currentBatch);
      reconcilePendingSize();
      if (LOGGING_ENABLED) log_stats();
      currentBatch = createNewBatch();
      txnsPerBatch = 0;
    }
    throttle();
  }

  private void throttle() {
    int diff = pending_size - slowDownStartsAt;
    if (diff >= 0) {
      long sleepTime = (long) (1 + diff * sleepTimeIncrements);
      try {
        wait(sleepTime);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private void reconcilePendingSize() {
    pending_size = pendingBatches.size();
  }

  private void put(ClientTransactionBatch batch) {
    try {
      pendingBatches.put(batch);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void log_stats() {
    int size = pending_size;
    if (size == MAX_PENDING_BATCHES) {
      logger.info("Max pending size reached !!! : Pending Batches size = " + size + " TxnsInBatch = " + txnsPerBatch);
    } else if (size % 5 == 0) {
      logger.info("Pending Batch Size : " + size + " TxnsInBatch = " + txnsPerBatch);
    }
  }

  private ClientTransactionBatch get() {
    try {
      return (ClientTransactionBatch) pendingBatches.poll(0);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private ClientTransactionBatch peek() {
    return (ClientTransactionBatch) pendingBatches.peek();
  }

  public ClientTransactionBatch getNextBatch() {
    ClientTransactionBatch batch = get();
    if (batch != null) return batch;
    synchronized (this) {
      // Check again to avoid sending the txn in the wrong order
      batch = get();
      reconcilePendingSize();
      notifyAll();
      if (batch != null) return batch;
      if (!currentBatch.isEmpty()) {
        batch = currentBatch;
        currentBatch = createNewBatch();
        return batch;
      }
      return null;
    }
  }

  /**
   * Used only for testing
   */
  public synchronized void clear() {
    while (get() != null) {
      // remove all pending
    }
    currentBatch = createNewBatch();
  }

  public SequenceID getNextSequenceID() {
    ClientTransactionBatch batch = peek();
    if (batch != null) return batch.getMinTransactionSequence();
    synchronized (this) {
      batch = peek();
      if (batch != null) return batch.getMinTransactionSequence();
      if (!currentBatch.isEmpty()) return currentBatch.getMinTransactionSequence();
      SequenceID currentSequenceID = new SequenceID(sequence.getCurrentSequence());
      return currentSequenceID.next();
    }
  }

}
