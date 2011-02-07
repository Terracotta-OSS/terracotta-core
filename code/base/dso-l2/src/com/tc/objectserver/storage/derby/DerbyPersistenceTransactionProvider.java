/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

import java.sql.Connection;

class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  protected final DerbyDBEnvironment                derbyDBEnv;
  protected final PersistenceTransaction            nullTxn;

  private final ThreadLocal<PersistenceTransaction> threadLocalTxn = new ThreadLocal<PersistenceTransaction>() {
                                                                     @Override
                                                                     protected PersistenceTransaction initialValue() {
                                                                       return createNewTransaction();
                                                                     }
                                                                   };

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
    nullTxn = createNewTransaction();
  }

  public PersistenceTransaction getOrCreateNewTransaction() {
    return threadLocalTxn.get();
  }

  private PersistenceTransaction createNewTransaction() {
    try {
      Connection connection = derbyDBEnv.createConnection();
      return new DerbyDBPersistenceTransaction(connection);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    return nullTxn;
  }
}
