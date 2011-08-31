/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

public final class BerkeleyDBPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private final Environment env;

  public BerkeleyDBPersistenceTransactionProvider(Environment env) {
    this.env = env;
  }

  public PersistenceTransaction newTransaction() {
    try {
      return new BerkeleyDBPersistenceTransaction(newNativeTransaction());
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  private Transaction newNativeTransaction() throws DatabaseException {
    return this.env.beginTransaction(null, null);
  }
}