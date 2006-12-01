/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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