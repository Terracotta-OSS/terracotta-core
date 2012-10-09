package com.tc.objectserver.persistence.gb;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import org.terracotta.corestorage.StorageManager;

/**
 * @author tim
 */
public class GBPersistenceTransactionProvider implements PersistenceTransactionProvider {

  private final StorageManager manager;

  public GBPersistenceTransactionProvider(StorageManager manager) {
    this.manager = manager;
  }

  @Override
  public PersistenceTransaction newTransaction() {
    return new GBTransaction();
  }


  private class GBTransaction implements PersistenceTransaction {
    private final Thread t;

    private GBTransaction() {
      this.t = Thread.currentThread();
      manager.begin();
    }

    @Override
    public Object getTransaction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
      if (Thread.currentThread() != t) {
        throw new IllegalStateException("Begin and commit threads don't match.");
      }
      manager.commit();
    }

    @Override
    public void abort() {
      throw new UnsupportedOperationException();
    }
  }
}
