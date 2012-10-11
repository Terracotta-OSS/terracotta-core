/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.wrapper;

import com.tc.objectserver.api.Transaction;

/**
 * Used for batching changes in a transaction
 */
public interface BatchedTransaction {

  /**
   * Start the batched transaction. Creates a new transaction for use
   */
  void startBatchedTransaction();

  /**
   * Returns the batchSize
   */
  int getBatchSize();

  /**
   * Get current transaction
   */
  Transaction getCurrentTransaction();

  /**
   * Commits current transaction and create new one if necessary; depending on the pending changes count. Otherwise
   * remembers the pending changes count
   */
  void optionalCommit(int changesCount);

  /**
   * Commits the underlying transaction if there are any more pending changes
   */
  long completeBatchedTransaction();

}