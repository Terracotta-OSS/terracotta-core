/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

class TransactionWrapper implements PersistenceTransaction {
  private final Transaction tx;

  public TransactionWrapper(Transaction tx) {
    this.tx = tx;
  }

  public Transaction getTransaction() {
    return tx;
  }

  public void commit() {
    if (tx != null) try {
      tx.commit();
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }
}