/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private final DerbyDBEnvironment                  derbyDBEnv;
  private final ThreadLocal<PersistenceTransaction> threadLocalTxn = new ThreadLocal<PersistenceTransaction>() {
                                                                     @Override
                                                                     protected PersistenceTransaction initialValue() {
                                                                       return createNewTransaction();
                                                                     }
                                                                   };

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
  }

  public PersistenceTransaction newTransaction() {
    return threadLocalTxn.get();
  }

  private PersistenceTransaction createNewTransaction() {
    try {
      return new DerbyDBPersistenceTransaction(derbyDBEnv.createConnection());
    } catch (Exception e) {
      throw new DBException(e);
    }
  }
}
