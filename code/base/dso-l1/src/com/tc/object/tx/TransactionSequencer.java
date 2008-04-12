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
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.Util;

public class TransactionSequencer {

  private static final TCLogger         logger         = TCLogging.getLogger(TransactionSequencer.class);

  private static final boolean          LOGGING_ENABLED;
  private static final int              MAX_BYTE_SIZE_FOR_BATCH;
  private static final int              MAX_PENDING_BATCHES;
  private static final long             MAX_SLEEP_TIME_BEFORE_HALT;

  static {
    // Set the values from the properties here.
    LOGGING_ENABLED = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_LOGGING_ENABLED);
    MAX_BYTE_SIZE_FOR_BATCH = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXBATCHSIZE_INKILOBYTES) * 1024;
    MAX_PENDING_BATCHES = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES);
    MAX_SLEEP_TIME_BEFORE_HALT = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXSLEEPTIME_BEFOREHALT);
  }

  private final SequenceGenerator       sequence       = new SequenceGenerator(1);
  private final TransactionBatchFactory batchFactory;
  private final BoundedLinkedQueue      pendingBatches = new BoundedLinkedQueue(MAX_PENDING_BATCHES);

  private ClientTransactionBatch        currentBatch;
  private int                           pending_size   = 0;

  private int                           slowDownStartsAt;
  private double                        sleepTimeIncrements;
  private int                           txnsPerBatch   = 0;
  private boolean                       shutdown       = false;

  private final LockAccounting          lockAccounting;
  private final SampledCounter          numTransactionsCounter;
  private final SampledCounter          numBatchesCounter;
  private final SampledCounter          batchSizeCounter;
  private final SampledCounter          pendingTransactionsSize;

  public TransactionSequencer(TransactionBatchFactory batchFactory, LockAccounting lockAccounting,
                              SampledCounter numTransactionCounter, SampledCounter numBatchesCounter,
                              SampledCounter batchSizeCounter, SampledCounter pendingTransactionsSize) {
    this.batchFactory = batchFactory;
    this.lockAccounting = lockAccounting;
    this.currentBatch = createNewBatch();
    this.slowDownStartsAt = (int) (MAX_PENDING_BATCHES * 0.66);
    this.sleepTimeIncrements = MAX_SLEEP_TIME_BEFORE_HALT / (MAX_PENDING_BATCHES - slowDownStartsAt);
    if (LOGGING_ENABLED) log_settings();
    this.numBatchesCounter = numBatchesCounter;
    this.numTransactionsCounter = numTransactionCounter;
    this.batchSizeCounter = batchSizeCounter;
    this.pendingTransactionsSize = pendingTransactionsSize;
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

  private boolean addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    return batch.addTransaction(txn, sequence);
  }

  public synchronized void addTransaction(ClientTransaction txn) {
    if (shutdown) {
      logger.error("Sequencer shutdown. Not committing " + txn);
    }

    try {
      addTxnInternal(txn);
    } catch (Throwable t) {
      // logging of exceptions is done at a higher level
      shutdown = true;
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  public synchronized void shutdown() {
    shutdown = true;
  }

  /**
   * XXX::Note : There is automatic throttling built in by adding to a BoundedLinkedQueue from within a synch block
   */
  private void addTxnInternal(ClientTransaction txn) {
    txnsPerBatch++;
    numTransactionsCounter.increment();

    boolean folded = addTransactionToBatch(txn, currentBatch);

    batchSizeCounter.setValue(currentBatch.byteSize());

    if (!txn.isConcurrent() && !folded) {
      // It is important to add the lock accounting before exposing the current batch to be sent (ie. put() below)
      lockAccounting.add(txn.getTransactionID(), txn.getAllLockIDs());
    }

    if (currentBatch.byteSize() > MAX_BYTE_SIZE_FOR_BATCH) {
      put(currentBatch);
      reconcilePendingSize();
      if (LOGGING_ENABLED) log_stats();
      currentBatch = createNewBatch();
      txnsPerBatch = 0;
      // do not set the value of numTransactionsCounter to zero here, as it will be sampled every second (the frequency
      // of the SampledCounter)
      numBatchesCounter.increment();
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
    pendingTransactionsSize.setValue(pending_size);
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
    boolean isInterrupted = false;
    ClientTransactionBatch returnValue = null;
    while (true) {
      try {
        returnValue = (ClientTransactionBatch) pendingBatches.poll(0);
        break;
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return returnValue;
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
