/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.tx.ServerTransactionID;

import java.util.LinkedHashMap;

public class IncomingTransactionBatchContext implements TransactionBatchContext {

  private final CommitTransactionMessage                              ctm;
  private final NodeID                                                nodeID;
  private final LinkedHashMap<ServerTransactionID, ServerTransaction> txns;
  private final long[]                                                highWatermark;

  public IncomingTransactionBatchContext(NodeID nodeID, CommitTransactionMessage ctm,
                                         LinkedHashMap<ServerTransactionID, ServerTransaction> txns,
                                         long[] highWatermark) {
    this.nodeID = nodeID;
    this.ctm = ctm;
    this.txns = txns;
    this.highWatermark = highWatermark;
  }

  public CommitTransactionMessage getCommitTransactionMessage() {
    return ctm;
  }

  public NodeID getSourceNodeID() {
    return nodeID;
  }

  public long[] getHighWatermark() {
    return highWatermark;
  }

  public int getNumTxns() {
    return txns.size();
  }

  public LinkedHashMap<ServerTransactionID, ServerTransaction> getTransactions() {
    return txns;
  }

}
