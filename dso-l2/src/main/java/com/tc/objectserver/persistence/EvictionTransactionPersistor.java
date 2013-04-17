package com.tc.objectserver.persistence;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.TransactionBatchContext;

import java.util.Collection;


public interface EvictionTransactionPersistor {

  public void saveTransactionBatch(ServerTransactionID serverTransactionID, TransactionBatchContext transactionBatchContext);

  public TransactionBatchContext getTransactionBatch(ServerTransactionID serverTransactionID);

  public void removeTransaction(ServerTransactionID serverTransactionID);

  public Collection<TransactionBatchContext> getAllTransactionBatches();

  public void removeAllTransactions();

}
