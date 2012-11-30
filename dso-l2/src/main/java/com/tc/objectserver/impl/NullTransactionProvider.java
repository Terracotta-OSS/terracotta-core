/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionListener;
import com.tc.objectserver.api.TransactionProvider;

public class NullTransactionProvider implements TransactionProvider {

  private static final Transaction NULL_TRANSACTION = new NullPersistenceTransaction();

  public Object getTransaction() {
    return null;
  }

  public Transaction newTransaction() {
    return NULL_TRANSACTION;
  }

  private final static class NullPersistenceTransaction implements Transaction {
    public void commit() {
      return;
    }

    public void abort() {
      //
    }

        @Override
        public void addTransactionListener(TransactionListener l) {

        }

    public Object getTransaction() {
      return null;
    }

  }
}