package com.tc.objectserver.persistence;


import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.TransactionBatchContext;

import java.util.Collection;
import java.util.HashSet;

public class NullEvictionTransactionPersistorImpl implements EvictionTransactionPersistor {

  public NullEvictionTransactionPersistorImpl() {
  }

  @Override
  public TransactionBatchContext getTransactionBatch(ServerTransactionID serverTransactionID) {
    return null;
  }

  @Override
  public void saveTransactionBatch(ServerTransactionID serverTransactionID, TransactionBatchContext transactionBatchContext) {

  }

  @Override
  public void removeTransaction(ServerTransactionID serverTransactionID) {
  }

  @Override
  public Collection<TransactionBatchContext> getAllTransactionBatches() {
    return new HashSet<TransactionBatchContext>();
  }

  @Override
  public void removeAllTransactions() {

  }
}
