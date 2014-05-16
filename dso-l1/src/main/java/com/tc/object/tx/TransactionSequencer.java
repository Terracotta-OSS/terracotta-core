/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.ClearableCallback;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.Util;

import java.util.LinkedList;

public class TransactionSequencer implements ClearableCallback {

  private static final TCLogger                             logger         = TCLogging
                                                                               .getLogger(TransactionSequencer.class);

  private static final boolean                              LOGGING_ENABLED;
  private static final int                                  MAX_BYTE_SIZE_FOR_BATCH;
  private static final int                                  MAX_PENDING_BATCHES;
  private static final long                                 MAX_SLEEP_TIME_BEFORE_HALT;
  private static final int                                  MIN_AVG_TRANSACTION_SIZE = 500;

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

  private SequenceGenerator                                 sequence       = new SequenceGenerator(1);
  private final TransactionBatchFactory                     batchFactory;
  private final LinkedList<ClientTransactionBatch>       pendingBatches = new LinkedList<ClientTransactionBatch>();
  private int                                               waiters = 0;

  private ClientTransactionBatch                            currentBatch;
  private final Average                                           currentWritten = new Average();

  private final int                                         slowDownStartsAt;
  private final double                                      sleepTimeIncrements;
  private int                                               txnsPerBatch   = 0;
  private volatile boolean                                  shutdown       = false;

  private final LockAccounting                              lockAccounting;
  private final SampledRateCounter                          transactionSizeCounter;
  private final SampledRateCounter                          transactionsPerBatchCounter;

  private final GroupID                                     groupID;
  private final TransactionIDGenerator                      transactionIDGenerator;
  private final AbortableOperationManager                   abortableOperationManager;
  private final RemoteTransactionManagerImpl                remoteTxnMgrImpl;

  public TransactionSequencer(GroupID groupID, TransactionIDGenerator transactionIDGenerator,
                              TransactionBatchFactory batchFactory, LockAccounting lockAccounting,
                              SampledRateCounter transactionSizeCounter,
                              SampledRateCounter transactionsPerBatchCounter,
                              AbortableOperationManager abortableOperationManager,
                              RemoteTransactionManagerImpl remoteTxnMgrImpl) {

    this.groupID = groupID;
    this.transactionIDGenerator = transactionIDGenerator;
    this.batchFactory = batchFactory;
    this.lockAccounting = lockAccounting;
    createNewBatch();
    this.slowDownStartsAt = (int) (MAX_PENDING_BATCHES / 2);
    this.sleepTimeIncrements = MAX_SLEEP_TIME_BEFORE_HALT / (MAX_PENDING_BATCHES - this.slowDownStartsAt);
    if (LOGGING_ENABLED) {
      log_settings();
    }
    this.transactionSizeCounter = transactionSizeCounter;
    this.transactionsPerBatchCounter = transactionsPerBatchCounter;
    this.abortableOperationManager = abortableOperationManager;
    this.remoteTxnMgrImpl = remoteTxnMgrImpl;
  }

  @Override
  public synchronized void cleanup() {
    sequence = new SequenceGenerator(1);
    pendingBatches.clear();
    lockAccounting.cleanup();
    createNewBatch();
    notifyAll();
  }
  
  private void log_settings() {
    logger.info("Max Byte Size for Batches = " + MAX_BYTE_SIZE_FOR_BATCH + " Max Pending Batches = "
                + MAX_PENDING_BATCHES);
    logger.info("Max Sleep time = " + MAX_SLEEP_TIME_BEFORE_HALT + " Slow down starts at = " + this.slowDownStartsAt
                + " sleep time increments = " + this.sleepTimeIncrements);
  }
  
  int getMaxPendingSize() {
      return MAX_PENDING_BATCHES;
  }

  private void createNewBatch() {
    this.currentBatch = this.batchFactory.nextBatch(this.groupID);
  }

  public void addTransaction(ClientTransaction txn) {
    if (this.shutdown) {
      logger.error("Sequencer shutdown. Not committing " + txn);
    }

    try {
      addTxnInternal(txn);
    } catch (final Throwable t) {
      // logging of exceptions is done at a higher level
      this.shutdown = true;
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  public boolean throttleIfNecesary() throws AbortedOperationException {
    int diff = this.pendingBatches.size() - this.slowDownStartsAt;
    if (diff >= 0) {
        waitIfNecessary();
        return true;
    } else {
      return false;
    }
  }

  public synchronized void shutdown() {
    this.shutdown = true;
  }

  private void addTxnInternal(ClientTransaction txn) {
    // waitIfNecessary();
    final TransactionID txnID = addToCurrentBatch(txn);

    synchronized (transactionSizeCounter) {
      // transactionSize = batchSize / number of transactions
      this.transactionSizeCounter.setNumeratorValue(this.currentBatch.byteSize());
      this.transactionSizeCounter.increment(0, 1);
    }
    
    if (txnID.isNull()) { throw new AssertionError("Transaction id is null"); }
  }
  
  private TransactionID addToCurrentBatch(ClientTransaction txn) {
    int numTransactionsDelta = 1;
    int numBatchesDelta = 0;
    int written = 0;
    TransactionBuffer buffer;
    
    try {
      synchronized (this) {
        if ( this.currentBatch.numberOfTxnsBeforeFolding() > this.getAverageBatchSize() ) {
          if ( this.currentBatch.numberOfTxnsBeforeFolding() == 0 ) {
            throw new AssertionError("no transaction in batch " + this.currentWritten + " " + this.currentBatch);
          }
          this.pendingBatches.add(this.currentBatch);
          if (LOGGING_ENABLED) {
            log_stats();
          }
          createNewBatch();
          this.txnsPerBatch = 0;
          numBatchesDelta = 1;
        }

        this.txnsPerBatch += 1;

        if ( this.batchFactory.isFoldingSupported() ) {
          written = this.currentBatch.byteSize();
          FoldedInfo fold = this.currentBatch.addTransaction(txn, sequence, transactionIDGenerator);
          this.lockAccounting.add(fold.getFoldedTransactionID(), txn.getAllLockIDs());
          numTransactionsDelta = 0;
          written = this.currentBatch.byteSize() - written;
          //  if the transaction is folded, it's already written.  return the transaction id
          return fold.getFoldedTransactionID();
        } else {
          SequenceID sid = new SequenceID(this.sequence.getNextSequence());
          TransactionID tid = transactionIDGenerator.nextTransactionID();
          txn.setSequenceID(sid);
          txn.setTransactionID(tid);
          buffer = this.currentBatch.addSimpleTransaction(txn);
          this.lockAccounting.add(tid, txn.getAllLockIDs());
        }
      }

      written = buffer.write(txn);

      return txn.getTransactionID();
    } finally {
      this.currentWritten.written(written);
      this.transactionsPerBatchCounter.increment(numTransactionsDelta, numBatchesDelta);
      synchronized (transactionSizeCounter) {
          // transactionSize = batchSize / number of transactions
        this.transactionSizeCounter.setNumeratorValue(written);
        this.transactionSizeCounter.increment(0, 1);
      }
    }
  }

  private synchronized void waitIfNecessary() throws AbortedOperationException {
    boolean isInterrupted = false;
    try {
      do {
        if (remoteTxnMgrImpl.isRejoinInProgress()) { throw new PlatformRejoinException(); }
        int diff = this.pendingBatches.size() - this.slowDownStartsAt;
        if (diff >= 0) {
          long sleepTime = (long) (1 + diff * this.sleepTimeIncrements);
          try {
              waiters++;
              wait(sleepTime);
          } catch (InterruptedException e) {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
            isInterrupted = true;
          } finally {
            waiters--;            
          }
        }
      } while (this.pendingBatches.size() >= MAX_PENDING_BATCHES);
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }
  
  private void log_stats() {
    int size = this.pendingBatches.size();
    if (size == MAX_PENDING_BATCHES) {
      logger.info("Max pending size reached !!! : Pending Batches size = " + size + " TxnsInBatch = "
                  + this.txnsPerBatch);
    } else if (size % 5 == 0) {
      logger.info("Pending Batch Size : " + size + " TxnsInBatch = " + this.txnsPerBatch + " remote " + remoteTxnMgrImpl);
    }
  }

  private ClientTransactionBatch peek() {
    return this.pendingBatches.peek();
  }

  public ClientTransactionBatch getNextBatch() {
    ClientTransactionBatch batch = null;
    try {
      synchronized (this) {
        batch = this.pendingBatches.poll();
        if (batch != null) { 
          if ( waiters > 0 ) {
            notify();
          }
          return batch; 
        } else if (!this.currentBatch.isEmpty()) {
          batch = this.currentBatch;
          createNewBatch();
          return batch;
        } else {
        return null;
      }
      }
    } finally {
      if ( LOGGING_ENABLED && batch != null && logger.isDebugEnabled() ) {
        logger.debug("batch count: " + batch.numberOfTxnsBeforeFolding() + " average size: " + this.currentWritten.getAverage());
      }
    }
  }

  /**
   * Used only for testing
   */
  public synchronized void clear() {
    this.pendingBatches.clear();
    createNewBatch();
    notifyAll();
  }
  
  boolean isEmpty() {
    return this.pendingBatches.isEmpty();
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
  
  public int getAverageBatchSize() {
    return MAX_BYTE_SIZE_FOR_BATCH / currentWritten.getAverage();
  }
    
  private static class Average {
    private int count = 0;
    private int written = 0;
    
    public synchronized void written(int w) {
      written += w;
      if ( count++ == 1024 ) {
        rebalance();
      }
    }
    
    public synchronized int getAverage() {
      if ( count == 0 ) {
        return MIN_AVG_TRANSACTION_SIZE;
      }
      int ave = written / count;
      if ( ave < MIN_AVG_TRANSACTION_SIZE ) {
        return MIN_AVG_TRANSACTION_SIZE;
      } else {
        return ave;
      }
    }
    
    private void rebalance() {
      written = written / count;
      count = 1;
    }
  }

}
