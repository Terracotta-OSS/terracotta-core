/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.storage.api.PersistenceTransaction;

public abstract class AbstractBerkeleyDatabase {
  protected final Database db;

  public AbstractBerkeleyDatabase(Database db) {
    this.db = db;
  }

  protected Transaction pt2nt(PersistenceTransaction tx) {
    Object o = (tx != null) ? tx.getTransaction() : null;
    if (o != null) {
      if (!(o instanceof Transaction)) { throw new AssertionError("Invalid transaction from " + tx + ": " + o); }
      return (Transaction) o;
    } else {
      return null;
    }
  }

  public final Database getDatabase() {
    return db;
  }
}
