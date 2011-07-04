/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.Assert;

/**
 * This class is not thread-safe and not intended for use by multiple threads
 */
public class BatchedTransactionImpl implements BatchedTransaction {

  private final PersistenceTransactionProvider transactionProvider;
  private final int                            batchSize;
  private int                                  pendingChangesCount;
  private long                                 totalChangesCount;
  private PersistenceTransaction               currentTransaction;

  public BatchedTransactionImpl(PersistenceTransactionProvider ptp, int batchSize) {
    Assert.assertNotNull(ptp);
    if (batchSize <= 0) {
      //
      throw new AssertionError("Batch size should be greater than 0");
    }
    this.transactionProvider = ptp;
    this.batchSize = batchSize;
  }

  public void startBatchedTransaction() {
    if (pendingChangesCount != 0 || totalChangesCount != 0) {
      // sane formatter
      throw new AssertionError("BatchedTransaction cannot be reused, pendingChangesCount: " + pendingChangesCount
                               + ", totalChangesCount: " + totalChangesCount);
    }
    currentTransaction = transactionProvider.newTransaction();
  }

  public PersistenceTransaction getCurrentTransaction() {
    check();
    return currentTransaction;
  }

  private void check() throws AssertionError {
    if (currentTransaction == null) {
      // sane formatter
      throw new AssertionError("startBatchedTransaction() needs to be called before this");
    }
  }

  public void optionalCommit(int changesCount) {
    check();
    totalChangesCount += changesCount;
    pendingChangesCount += changesCount;
    if (pendingChangesCount >= batchSize) {
      pendingChangesCount = 0;
      currentTransaction.commit();
      currentTransaction = transactionProvider.newTransaction();
    }
  }

  public long completeBatchedTransaction() {
    check();
    if (pendingChangesCount > 0) {
      pendingChangesCount = 0;
      currentTransaction.commit();
      currentTransaction = null;
    }
    return totalChangesCount;
  }

  public int getBatchSize() {
    return batchSize;
  }

}
