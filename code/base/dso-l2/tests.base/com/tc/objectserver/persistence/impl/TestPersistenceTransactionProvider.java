/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

public final class TestPersistenceTransactionProvider implements PersistenceTransactionProvider {

  public final LinkedQueue nullTransactionContexts = new LinkedQueue();
  public final LinkedQueue newTransactions         = new LinkedQueue();

  public PersistenceTransaction getOrCreateNewTransaction() {
    try {
      PersistenceTransaction rv = new TestPersistenceTransaction();
      newTransactions.put(rv);
      return rv;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    try {
      nullTransactionContexts.put(new Object());
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
    return TestPersistenceTransaction.NULL_TRANSACTION;
  }

  public PersistenceTransaction createNewTransaction() {
    return getOrCreateNewTransaction();
  }

}