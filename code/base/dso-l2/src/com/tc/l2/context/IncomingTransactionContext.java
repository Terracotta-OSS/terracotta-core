/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.net.groups.NodeID;
import com.tc.object.msg.CommitTransactionMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class IncomingTransactionContext implements EventContext {

  private final CommitTransactionMessage ctm;
  private final Collection               txns;
  private final Set                      serverTxnIDs;
  private final NodeID                   nodeID;

  public IncomingTransactionContext(NodeID nodeID, CommitTransactionMessage ctm, Map txnsMap) {
    this.nodeID = nodeID;
    this.ctm = ctm;
    this.txns = txnsMap.values();
    this.serverTxnIDs = txnsMap.keySet();
  }

  public CommitTransactionMessage getCommitTransactionMessage() {
    return ctm;
  }

  public Set getServerTransactionIDs() {
    return serverTxnIDs;
  }

  public Collection getTxns() {
    return txns;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

}
