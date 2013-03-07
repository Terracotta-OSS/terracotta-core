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

  @Override
  public Transaction newTransaction() {
    return new StorageTransaction();
  }

  private class StorageTransaction implements Transaction {
    private StorageTransaction() {
      manager.begin();
    }

    @Override
    public void commit() {
      manager.commit();
    }
  }
}
