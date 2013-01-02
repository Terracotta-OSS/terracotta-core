/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.l2.ha.TransactionBatchListener;
import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.tx.TransactionID;

import java.util.concurrent.LinkedBlockingQueue;

public class TestTransactionBatchManager implements TransactionBatchManager {

  LinkedBlockingQueue txns = new LinkedBlockingQueue();

  public void addTransactionBatch(CommitTransactionMessageImpl ctm) {
    throw new ImplementMe();
  }

  @Override
  public boolean batchComponentComplete(NodeID committerID, TransactionID txnID) {
    throw new ImplementMe();
  }

  @Override
  public void defineBatch(NodeID node, int numTxns) {
    throw new ImplementMe();
  }

  @Override
  public void shutdownNode(NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void addTransactionBatch(CommitTransactionMessage ctm) {
    throw new ImplementMe();
  }

  @Override
  public void processTransactions(TransactionBatchContext incomingTransactionBatchContext) {
    this.txns.add(incomingTransactionBatchContext);
  }

  @Override
  public void nodeConnected(NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark) {
    // Ignore
  }

  @Override
  public void registerForBatchTransaction(TransactionBatchListener listener) {
    throw new ImplementMe();
  }
}