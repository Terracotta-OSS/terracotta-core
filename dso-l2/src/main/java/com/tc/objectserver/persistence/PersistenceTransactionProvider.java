package com.tc.objectserver.persistence;

import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;

import org.terracotta.persistence.IPersistentStorage;

/**
 * @author tim
 */
public class PersistenceTransactionProvider implements TransactionProvider {
  private final IPersistentStorage manager;

  public PersistenceTransactionProvider(IPersistentStorage manager) {
    this.manager = manager;
  }

  @Override
  public Transaction newTransaction() {
    return new StorageTransaction();
  }

  private class StorageTransaction implements Transaction {
    
    IPersistentStorage.Transaction transaction = manager.begin();

    @Override
    public void commit() {
      transaction.commit();
    }
  }
}
