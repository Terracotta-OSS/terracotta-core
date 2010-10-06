/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PassiveTransactionAccount implements TransactionAccount {

  private final NodeID       nodeID;
  private final Set          txnIDs = Collections.synchronizedSet(new HashSet());
  private CallBackOnComplete callback;
  private boolean            dead   = false;

  public PassiveTransactionAccount(final NodeID source) {
    this.nodeID = source;
  }

  public void addWaitee(final NodeID waitee, final TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server : " + waitee + " , "
                             + requestID);
  }

  public boolean applyCommitted(final TransactionID requestID) {
    synchronized (this.txnIDs) {
      this.txnIDs.remove(new ServerTransactionID(this.nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
    return true;
  }

  public boolean broadcastCompleted(final TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  public NodeID getNodeID() {
    return this.nodeID;
  }

  public boolean hasWaitees(final TransactionID requestID) {
    return false;
  }

  public boolean removeWaitee(final NodeID waitee, final TransactionID requestID) {
    throw new AssertionError("Transactions should never be ACKED to Passive Server");
  }

  public Set requestersWaitingFor(final NodeID waitee) {
    return Collections.EMPTY_SET;
  }

  public boolean skipApplyAndCommit(final TransactionID requestID) {
    synchronized (this.txnIDs) {
      this.txnIDs.remove(new ServerTransactionID(this.nodeID, requestID));
      invokeCallBackOnCompleteIfNecessary();
    }
    return true;
  }

  public boolean relayTransactionComplete(final TransactionID requestID) {
    throw new AssertionError("Transactions should never be relayed from Passive Server");
  }

  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    throw new AssertionError("Objects can't be synced from Passive Server");
  }

  public void addAllPendingServerTransactionIDsTo(final Set txnsInSystem) {
    synchronized (this.txnIDs) {
      txnsInSystem.addAll(this.txnIDs);
    }
  }

  public void incommingTransactions(final Set serverTxnsIDs) {
    this.txnIDs.addAll(serverTxnsIDs);
  }

  public void nodeDead(final CallBackOnComplete callBack) {
    synchronized (this.txnIDs) {
      this.callback = callBack;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (this.dead && this.txnIDs.isEmpty()) {
      this.callback.onComplete(this.nodeID);
    }
  }

  @Override
  public String toString() {
    return "PassiveTransactionAccount [ " + this.nodeID + " ] = " + this.txnIDs;
  }
}
