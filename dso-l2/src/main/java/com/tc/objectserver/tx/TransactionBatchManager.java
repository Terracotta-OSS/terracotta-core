/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.l2.ha.TransactionBatchListener;
import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.tx.TransactionID;

public interface TransactionBatchManager {

  public void addTransactionBatch(CommitTransactionMessage ctm);

  public void defineBatch(NodeID node, int numTxns) throws BatchDefinedException;

  public boolean batchComponentComplete(NodeID committerID, TransactionID txnID) throws NoSuchBatchException;

  public void nodeConnected(NodeID nodeID);

  public void shutdownNode(NodeID nodeID);

  public void processTransactions(TransactionBatchContext batchContext);

  public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark);
  
  public void registerForBatchTransaction(TransactionBatchListener listener);

}