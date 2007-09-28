/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PassiveTransactionAccount implements TransactionAccount {

  private final NodeID nodeID;
  private final Set    txnIDs = Collections.synchronizedSet(new HashSet());

  public PassiveTransactionAccount(NodeID source) {
    this.nodeID = source;
  }

  public void addWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server : " + waitee + " , "
                             + requestID);
  }

  public boolean applyCommitted(TransactionID requestID) {
    txnIDs.remove(new ServerTransactionID(nodeID, requestID));
    return true;
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public boolean hasWaitees(TransactionID requestID) {
    return false;
  }

  public boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be ACKED to Passive Server");
  }

  public Set requestersWaitingFor(NodeID waitee) {
    return Collections.EMPTY_SET;
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    txnIDs.remove(new ServerTransactionID(nodeID, requestID));
    return true;
  }

  public boolean relayTransactionComplete(TransactionID requestID) {
    throw new AssertionError("Transactions should never be relayed from Passive Server");
  }

  public void addAllPendingServerTransactionIDsTo(HashSet txnsInSystem) {
    synchronized (txnIDs) {
      txnsInSystem.addAll(txnIDs);
    }
  }

  public void incommingTransactions(Set serverTxnsIDs) {
    txnIDs.addAll(serverTxnsIDs);
  }

  public void nodeDead(CallBackOnComplete callBack) {
    throw new AssertionError("This should never be called.");
  }
}
