/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;

import java.util.Collection;

/**
 * Client representation of a batch of transactions. Has methods that are only useful in a client context.
 */
public interface ClientTransactionBatch extends TransactionBatch {

  public TxnBatchID getTransactionBatchID();

  /**
   * Adds the collection of transaction ids in this batch to the given collection and returns it.
   */
  public Collection addTransactionIDsTo(Collection c);

  /**
   * Add the given transaction to this batch.
   * 
   * @return true if the transaction was folded
   */
  public FoldedInfo addTransaction(ClientTransaction txn, SequenceGenerator sequenceGenerator,
                                TransactionIDGenerator transactionIDGenerator);

  public TransactionBuffer removeTransaction(TransactionID txID);

  /**
   * Send the transaction to the server.
   */
  public void send();

  public int numberOfTxnsBeforeFolding();

  public int byteSize();

  public boolean isNull();

  public SequenceID getMinTransactionSequence();

  public Collection addTransactionSequenceIDsTo(Collection sequenceIDs);

  // For testing
  public String dump();
}
