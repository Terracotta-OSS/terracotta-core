/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

  private final NodeID       nodeID;
  private final Set          txnIDs = Collections.synchronizedSet(new HashSet());
  private CallBackOnComplete callback;
  private boolean            dead = false;

  public PassiveTransactionAccount(NodeID source) {
    this.nodeID = source;
  }

  public void addWaitee(NodeID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server : " + waitee + " , "
                             + requestID);
  }

  public boolean applyCommitted(TransactionID requestID) {
    synchronized (txnIDs) {
      txnIDs.remove(new ServerTransactionID(nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
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
    synchronized (txnIDs) {
      txnIDs.remove(new ServerTransactionID(nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
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
    synchronized (txnIDs) {
      this.callback = callBack;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && txnIDs.isEmpty()) {
      callback.onComplete(nodeID);
    }
  }
}
