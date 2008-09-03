/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ObjectSynchTransactionAccount implements TransactionAccount {

  private final NodeID sourceID;
  private final Map    txn2Waitees = new HashMap();

  public ObjectSynchTransactionAccount(NodeID sourceID) {
    this.sourceID = sourceID;
  }

  public synchronized void addAllPendingServerTransactionIDsTo(Set txnIDs) {
    for (Iterator i = txn2Waitees.keySet().iterator(); i.hasNext();) {
      TransactionID txnID = (TransactionID) i.next();
      txnIDs.add(new ServerTransactionID(sourceID, txnID));
    }
  }

  /**
   * Even though the current logic is we only send 1 txn to 1 node during object sync, it is managed in a set for safe
   * measures.
   */
  public synchronized void addWaitee(NodeID waitee, TransactionID requestID) {
    assert waitee.getType() == NodeID.L2_NODE_TYPE;
    Set waitees = getOrCreate(requestID);
    waitees.add(waitee);
  }

  private Set getOrCreate(TransactionID requestID) {
    Set waitees = (Set) txn2Waitees.get(requestID);
    if (waitees == null) {
      txn2Waitees.put(requestID, (waitees = new HashSet(2)));
    }
    return waitees;
  }

  public boolean applyCommitted(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  public NodeID getNodeID() {
    return sourceID;
  }

  public synchronized boolean hasWaitees(TransactionID requestID) {
    return txn2Waitees.containsKey(requestID);
  }

  public void incommingTransactions(Set serverTxnIDs) {
    throw new UnsupportedOperationException();
  }

  public void nodeDead(CallBackOnComplete callBack) {
    // This should never be called as record is only created for local node
    throw new UnsupportedOperationException();

  }

  public boolean relayTransactionComplete(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  public synchronized boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    assert waitee.getType() == NodeID.L2_NODE_TYPE;
    Set waiteesSet = (Set) txn2Waitees.get(requestID);
    if (waiteesSet == null) { return true; }
    waiteesSet.remove(waitee);
    if (waiteesSet.isEmpty()) {
      txn2Waitees.remove(requestID);
      return true;
    }
    return false;
  }

  public Set requestersWaitingFor(NodeID nodeID) {
    if (nodeID.getType() == NodeID.L1_NODE_TYPE) { return Collections.EMPTY_SET; }
    synchronized (this) {
      Set requesters = new HashSet();
      for (Iterator i = txn2Waitees.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        Set waiteesSet = (Set) e.getValue();
        if (waiteesSet.contains(nodeID)) {
          TransactionID requester = (TransactionID) e.getKey();
          requesters.add(requester);
        }
      }
      return requesters;
    }
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "ObjectSynchTransactionAccount [ " + sourceID + " ] : { txn2Waitees  : " + txn2Waitees + " }";
  }
}
