/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;

public class BerkeleyDBPersistenceTransaction implements PersistenceTransaction {
  private final Transaction tx;
  private boolean           isCommitted = false;

  public BerkeleyDBPersistenceTransaction(Transaction tx) {
    this.tx = tx;
  }

  public Transaction getTransaction() {
    return tx;
  }

  public void commit() {
    if (tx != null) {
      try {
        tx.commit();
      } catch (Exception e) {
        throw new DBException(e);
      }
    }
    isCommitted = true;
  }

  public void abort() {
    if (isCommitted) { throw new AssertionError(
                                                "This transaction was already committed. Abort after commit is not allowed."); }

    if (tx != null) {
      tx.abort();
    }
  }
}