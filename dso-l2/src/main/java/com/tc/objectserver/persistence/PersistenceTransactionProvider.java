package com.tc.objectserver.persistence;

import org.terracotta.corestorage.StorageManager;

import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;

/**
 * @author tim
 */
public class PersistenceTransactionProvider implements TransactionProvider {

  private final StorageManager manager;

  public PersistenceTransactionProvider(StorageManager manager) {
    this.manager = manager;
  }

  public Transaction newTransaction() {
    return new StorageTransaction();
  }


  private class StorageTransaction implements Transaction {
    private final Thread t;

    private StorageTransaction() {
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
