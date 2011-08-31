/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.PersistenceTransaction;

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
  PersistenceTransaction getCurrentTransaction();

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
