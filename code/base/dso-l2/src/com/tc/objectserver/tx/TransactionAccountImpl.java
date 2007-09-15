/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An account of the state of a given transaction. Keeps track of the initiating client, the state of the transaction
 * (applied, committed, etc), clients the transaction has been broadcast to, and clients that have ACKed the
 * transaction.
 */
public class TransactionAccountImpl implements TransactionAccount {
  final ChannelID            clientID;
  private final Map          waitees = Collections.synchronizedMap(new HashMap());
  private boolean            dead    = false;
  private CallBackOnComplete callBack;

  public TransactionAccountImpl(ChannelID clientID) {
    this.clientID = clientID;
  }

  public String toString() {
    return "TransactionAccount[" + clientID + ": waitees=" + waitees + "]\n";
  }

  public ChannelID getClientID() {
    return clientID;
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
  public boolean removeWaitee(ChannelID waitee, TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);

    if (transactionRecord == null) return false;
    synchronized (transactionRecord) {
      transactionRecord.remove(waitee);
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public void addWaitee(ChannelID waitee, TransactionID requestID) {
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

  public Set requestersWaitingFor(ChannelID waitee) {
    Set requesters = new HashSet();
    synchronized (waitees) {
      for (Iterator i = new HashSet(waitees.keySet()).iterator(); i.hasNext();) {
        TransactionID requester = (TransactionID) i.next();
        if (getRecord(requester).contains(waitee)) {
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
        txnIDs.add(new ServerTransactionID(clientID, txnID));
      }
    }
  }

  public void clientDead(CallBackOnComplete cb) {
    synchronized (waitees) {
      this.callBack = cb;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (dead && waitees.isEmpty()) {
      callBack.onComplete(clientID);
    }
  }

}