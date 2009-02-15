/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.Counter;
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

  private final int                     slowDownStartsAt;
  private final double                  sleepTimeIncrements;
  private int                           txnsPerBatch   = 0;
  private boolean                       shutdown       = false;

  private final LockAccounting          lockAccounting;
  private final Counter                 pendingBatchesSize;
  private final SampledCounter          numTransactionsCounter;
  private final SampledCounter          numBatchesCounter;
  private final SampledCounter          batchSizeCounter;

  private final GroupID                 groupID;
  private final TransactionIDGenerator  transactionIDGenerator;

  public TransactionSequencer(GroupID groupID, TransactionIDGenerator transactionIDGenerator,
                              TransactionBatchFactory batchFactory, LockAccounting lockAccounting,
                              SampledCounter numTransactionCounter, SampledCounter numBatchesCounter,
                              SampledCounter batchSizeCounter, Counter pendingBatchesSize) {

    this.groupID = groupID;
    this.transactionIDGenerator = transactionIDGenerator;
    this.batchFactory = batchFactory;
    this.lockAccounting = lockAccounting;
    this.currentBatch = createNewBatch();
    this.slowDownStartsAt = (int) (MAX_PENDING_BATCHES * 0.66);
    this.sleepTimeIncrements = MAX_SLEEP_TIME_BEFORE_HALT / (MAX_PENDING_BATCHES - this.slowDownStartsAt);
    if (LOGGING_ENABLED) {
      log_settings();
    }
    this.numBatchesCounter = numBatchesCounter;
    this.numTransactionsCounter = numTransactionCounter;
    this.batchSizeCounter = batchSizeCounter;
    this.pendingBatchesSize = pendingBatchesSize;
  }

  private void log_settings() {
    logger.info("Max Byte Size for Batches = " + MAX_BYTE_SIZE_FOR_BATCH + " Max Pending Batches = "
                + MAX_PENDING_BATCHES);
    logger.info("Max Sleep time = " + MAX_SLEEP_TIME_BEFORE_HALT + " Slow down starts at = " + this.slowDownStartsAt
                + " sleep time increments = " + this.sleepTimeIncrements);
  }

  private ClientTransactionBatch createNewBatch() {
    return this.batchFactory.nextBatch(this.groupID);
  }

  private boolean addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    return batch.addTransaction(txn, this.sequence, this.transactionIDGenerator);
  }

  public synchronized void addTransaction(ClientTransaction txn) {
    if (this.shutdown) {
      logger.error("Sequencer shutdown. Not committing " + txn);
    }

    try {
      addTxnInternal(txn);
    } catch (Throwable t) {
      // logging of exceptions is done at a higher level
      this.shutdown = true;
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  public synchronized void shutdown() {
    this.shutdown = true;
  }

  /**
   * XXX::Note : There is automatic throttling built in by adding to a BoundedLinkedQueue from within a synch block
   */
  private void addTxnInternal(ClientTransaction txn) {
    this.txnsPerBatch++;
    this.numTransactionsCounter.increment();

    boolean folded = addTransactionToBatch(txn, this.currentBatch);

    this.batchSizeCounter.setValue(this.currentBatch.byteSize());

    if (!txn.isConcurrent() && !folded) {
      // It is important to add the lock accounting before exposing the current batch to be sent (ie. put() below)
      TransactionID tid = txn.getTransactionID();
      assert !tid.isNull();
      this.lockAccounting.add(tid, txn.getAllLockIDs());
    }

    if (this.currentBatch.byteSize() > MAX_BYTE_SIZE_FOR_BATCH) {
      put(this.currentBatch);
      reconcilePendingSize();
      if (LOGGING_ENABLED) {
        log_stats();
      }
      this.currentBatch = createNewBatch();
      this.txnsPerBatch = 0;
      // do not set the value of numTransactionsCounter to zero here, as it will be sampled every second (the frequency
      // of the SampledCounter)
      this.numBatchesCounter.increment();
    }
    throttle();
  }

  private void throttle() {
    int diff = this.pending_size - this.slowDownStartsAt;
    if (diff >= 0) {
      long sleepTime = (long) (1 + diff * this.sleepTimeIncrements);
      try {
        wait(sleepTime);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private void reconcilePendingSize() {
    this.pending_size = this.pendingBatches.size();
    this.pendingBatchesSize.setValue(this.pending_size);
  }

  private void put(ClientTransactionBatch batch) {
    try {
      this.pendingBatches.put(batch);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void log_stats() {
    int size = this.pending_size;
    if (size == MAX_PENDING_BATCHES) {
      logger.info("Max pending size reached !!! : Pending Batches size = " + size + " TxnsInBatch = "
                  + this.txnsPerBatch);
    } else if (size % 5 == 0) {
      logger.info("Pending Batch Size : " + size + " TxnsInBatch = " + this.txnsPerBatch);
    }
  }

  private ClientTransactionBatch get() {
    boolean isInterrupted = false;
    ClientTransactionBatch returnValue = null;
    while (true) {
      try {
        returnValue = (ClientTransactionBatch) this.pendingBatches.poll(0);
        break;
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return returnValue;
  }

  private ClientTransactionBatch peek() {
    return (ClientTransactionBatch) this.pendingBatches.peek();
  }

  public ClientTransactionBatch getNextBatch() {
    ClientTransactionBatch batch = get();
    if (batch != null) { return batch; }
    synchronized (this) {
      // Check again to avoid sending the txn in the wrong order
      batch = get();
      reconcilePendingSize();
      notifyAll();
      if (batch != null) { return batch; }
      if (!this.currentBatch.isEmpty()) {
        batch = this.currentBatch;
        this.currentBatch = createNewBatch();
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
    this.currentBatch = createNewBatch();
  }

  public SequenceID getNextSequenceID() {
    ClientTransactionBatch batch = peek();
    if (batch != null) { return batch.getMinTransactionSequence(); }
    synchronized (this) {
      batch = peek();
      if (batch != null) { return batch.getMinTransactionSequence(); }
      if (!this.currentBatch.isEmpty()) { return this.currentBatch.getMinTransactionSequence(); }
      SequenceID currentSequenceID = new SequenceID(this.sequence.getCurrentSequence());
      return currentSequenceID.next();
    }
  }

}
