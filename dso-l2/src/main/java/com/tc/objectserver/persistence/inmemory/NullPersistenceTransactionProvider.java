/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

public class NullPersistenceTransactionProvider implements PersistenceTransactionProvider {

  private static final PersistenceTransaction NULL_TRANSACTION = new NullPersistenceTransaction();

  public Object getTransaction() {
    return null;
  }

  public PersistenceTransaction newTransaction() {
    return NULL_TRANSACTION;
  }

  private final static class NullPersistenceTransaction implements PersistenceTransaction {
    public void commit() {
      return;
    }

    public void abort() {
      //
    }

    public Object getTransaction() {
      return null;
    }

  }
}
