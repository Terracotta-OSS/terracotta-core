/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.SequenceID;

import java.util.Collection;

/**
 * Client representation of a batch of transactions.  Has methods that are only useful in a client context.
 */
public interface ClientTransactionBatch extends TransactionBatch {

  public TxnBatchID getTransactionBatchID();

  /**
   * Adds the collection of transaction ids in this batch to the given collection and returns it.
   */
  public Collection addTransactionIDsTo(Collection c);

  /**
   * Add the given transaction to this batch.
   */
  public void addTransaction(ClientTransaction txn);

  public void removeTransaction(TransactionID txID);

  /**
   * Send the transaction to the server.
   */
  public void send();

  /**
   * Adds the set of acknowledged GlobalTransactionIDs to the batch
   */
  public void addAcknowledgedTransactionIDs(Collection acknowledged);

  public int numberOfTxns();

  public int byteSize();

  public boolean isNull();

  public SequenceID getMinTransactionSequence();

  public Collection addTransactionSequenceIDsTo(Collection sequenceIDs);

  // For testing
  public String dump();
}
