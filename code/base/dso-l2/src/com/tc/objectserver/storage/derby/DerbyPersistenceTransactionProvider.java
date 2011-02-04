/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

import java.sql.Connection;

class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  protected final DerbyDBEnvironment                   derbyDBEnv;
  protected final static DerbyDBPersistenceTransaction NULL_TX        = new DerbyDBPersistenceTransaction(null);

  private final ThreadLocal<PersistenceTransaction>    threadLocalTxn = new ThreadLocal<PersistenceTransaction>() {
                                                                        @Override
                                                                        protected PersistenceTransaction initialValue() {
                                                                          return createNewTransaction();
                                                                        }
                                                                      };

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
  }

  public PersistenceTransaction getOrCreateNewTransaction() {
    return threadLocalTxn.get();
  }

  public PersistenceTransaction createNewTransaction() {
    try {
      Connection connection = derbyDBEnv.createConnection();
      return new DerbyDBPersistenceTransaction(connection);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    return NULL_TX;
  }
}
