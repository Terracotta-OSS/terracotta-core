package com.tc.objectserver.persistence.gb;

import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;
import org.terracotta.corestorage.StorageManager;

/**
 * @author tim
 */
public class GBPersistenceTransactionProvider implements TransactionProvider {

  private final StorageManager manager;

  public GBPersistenceTransactionProvider(StorageManager manager) {
    this.manager = manager;
  }

  public Transaction newTransaction() {
    return new GBTransaction();
  }


  private class GBTransaction implements Transaction {
    private final Thread t;

    private GBTransaction() {
      this.t = Thread.currentThread();
      manager.begin();
    }

    public void commit() {
      if (Thread.currentThread() != t) {
        throw new IllegalStateException("Begin and commit threads don't match.");
      }
      manager.commit();
    }

    public void abort() {
      throw new UnsupportedOperationException();
    }
  }
}
