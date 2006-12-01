/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;

final class SleepycatPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private static final PersistenceTransaction NULL_TRANSACTION = new TransactionWrapper(null);
  private final Environment                   env;

  public SleepycatPersistenceTransactionProvider(Environment env) {
    this.env = env;
  }

  public PersistenceTransaction newTransaction() {
    try {
      return new TransactionWrapper(newNativeTransaction());
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    return NULL_TRANSACTION;
  }

  Transaction newNativeTransaction() throws DatabaseException {
    return this.env.beginTransaction(null, null);
  }
}