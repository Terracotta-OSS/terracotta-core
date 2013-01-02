/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;

public final class TestPersistenceTransactionProvider implements TransactionProvider {

  public final LinkedQueue nullTransactionContexts = new LinkedQueue();
  public final LinkedQueue newTransactions         = new LinkedQueue();

  @Override
  public Transaction newTransaction() {
    try {
      Transaction rv = new TestPersistenceTransaction();
      newTransactions.put(rv);
      return rv;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

    public Transaction currentTransaction() {
        return newTransaction();
    }
  
  
}