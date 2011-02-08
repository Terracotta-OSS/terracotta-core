/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private final DerbyConnectionPool derbyConnectionPool;

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyConnectionPool = new DerbyConnectionPool(derbyDBEnv);
  }

  public PersistenceTransaction newTransaction() {
    return derbyConnectionPool.getTransaction();
  }

  public static interface ConnectionCommitListener {
    void transactionCommitted(DerbyDBPersistenceTransaction derbyTxn);
  }
}
