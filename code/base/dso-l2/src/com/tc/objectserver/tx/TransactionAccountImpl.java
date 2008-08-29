/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * An account of the state of a given transaction. Keeps track of the initiating client, the state of the transaction
 * (applied, committed, etc), clients the transaction has been broadcast to, and clients that have ACKed the
 * transaction.
 */
public class TransactionAccountImpl implements TransactionAccount {
  final NodeID               sourceID;
  private final Map          waitees = Collections.synchronizedMap(new HashMap());
  private volatile boolean   dead    = false;
  private CallBackOnComplete callBack;

  public TransactionAccountImpl(NodeID source) {
    this.sourceID = source;
  }

  public String toString() {
    return "TransactionAccount[" + sourceID + ": waitees=" + waitees + "]\n";
  }

  public NodeID getNodeID() {
    return sourceID;
  }

  public void incommingTransactions(Set txnIDs) {
    Assert.assertFalse(dead);
    for (Iterator i = txnIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxnID = (ServerTransactionID) i.next();
      createRecord(stxnID.getClientTransactionID());
    }
  }

  private void createRecord(TransactionID txnID) {
    Object old = waitees.put(txnID, new TransactionRecord());
    Assert.assertNull(old);
  }

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);

    if (transactionRecord == null) return false;
    synchronized (transactionRecord) {
      transactionRecord.remove(waitee);
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public void addWaitee(NodeID waitee, TransactionID requestID) {
    TransactionRecord record = getRecord(requestID);
    synchronized (record) {
      boolean added = record.addWaitee(waitee);
      Assert.eval(added);
    }
  }

  private TransactionRecord getRecord(TransactionID requestID) {
    return (TransactionRecord) waitees.get(requestID);
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.applyAndCommitSkipped();
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public boolean applyCommitted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.applyCommitted();
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.broadcastCompleted();
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public boolean relayTransactionComplete(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.relayTransactionComplete();
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public boolean hasWaitees(TransactionID requestID) {
    TransactionRecord record = getRecord(requestID);
    if (record == null) return false;
    synchronized (record) {
      return !record.isEmpty();
    }
  }

  public Set requestersWaitingFor(NodeID waitee) {
    Set requesters = new HashSet();
    synchronized (waitees) {
      for (Iterator i = waitees.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        TransactionRecord record = (TransactionRecord) e.getValue();
        if (record.contains(waitee)) {
          TransactionID requester = (TransactionID) e.getKey();
          requesters.add(requester);
        }
      }
    }
    return requesters;
  }

  private boolean checkCompletedAndRemove(TransactionID requestID, TransactionRecord record) {
    synchronized (waitees) {
      if (record.isComplete()) {
        waitees.remove(requestID);
        invokeCallBackOnCompleteIfNecessary();
        return !dead;
      }
      return false;
    }
  }

  public void addAllPendingServerTransactionIDsTo(HashSet txnIDs) {
    synchronized (waitees) {
      for (Iterator i = waitees.keySet().iterator(); i.hasNext();) {
        TransactionID txnID = (TransactionID) i.next();
        txnIDs.add(new ServerTransactionID(sourceID, txnID));
      }
    }
  }

  public void nodeDead(CallBackOnComplete cb) {
    synchronized (waitees) {
      this.callBack = cb;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && waitees.isEmpty()) {
      callBack.onComplete(sourceID);
    }
  }

}