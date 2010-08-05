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
    return (tx != null) ? ((BerkeleyDBPersistenceTransaction) tx).getTransaction() : null;
  }

  public final Database getDatabase() {
    return db;
  }
}
