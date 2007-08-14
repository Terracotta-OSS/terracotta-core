/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.HashMap;
import java.util.Map;

class TransactionWrapper implements PersistenceTransaction {
  private final Transaction tx;
  private final Map         properties = new HashMap(1);

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

  public Object getProperty(Object key) {
    return properties.get(key);
  }

  public Object setProperty(Object key, Object value) {
    return properties.put(key, value);
  }
}