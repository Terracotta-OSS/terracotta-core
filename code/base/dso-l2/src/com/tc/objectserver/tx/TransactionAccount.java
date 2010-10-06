/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

public interface TransactionAccount {

  public NodeID getNodeID();

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(NodeID waitee, TransactionID txnID);

  public void addWaitee(NodeID waitee, TransactionID txnID);

  public boolean skipApplyAndCommit(TransactionID txnID);

  public boolean applyCommitted(TransactionID txnID);

  public boolean broadcastCompleted(TransactionID txnID);

  public boolean hasWaitees(TransactionID txnID);

  public Set requestersWaitingFor(NodeID nodeID);

  public boolean relayTransactionComplete(TransactionID txnID);

  public void incommingTransactions(Set serverTxnIDs);

  public void addAllPendingServerTransactionIDsTo(Set txnsInSystem);

  public void addObjectsSyncedTo(NodeID to, TransactionID txnID);

  public void nodeDead(CallBackOnComplete callBack);

  public interface CallBackOnComplete {
    public void onComplete(NodeID dead);
  }
}