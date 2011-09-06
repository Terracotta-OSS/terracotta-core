/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
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
  final NodeID                                        sourceID;
  private final Map<TransactionID, TransactionRecord> waitees = Collections
                                                                  .synchronizedMap(new HashMap<TransactionID, TransactionRecord>());
  private volatile boolean                            dead    = false;
  private CallBackOnComplete                          callBack;

  public TransactionAccountImpl(final NodeID source) {
    this.sourceID = source;
  }

  public NodeID getNodeID() {
    return this.sourceID;
  }

  public void incomingTransactions(Set txnIDs) {
    Assert.assertFalse(this.dead);
    for (final Iterator i = txnIDs.iterator(); i.hasNext();) {
      final ServerTransactionID stxnID = (ServerTransactionID) i.next();
      createRecord(stxnID.getClientTransactionID(), new TransactionRecord());
    }
  }

  private void createRecord(final TransactionID txnID, final TransactionRecord transactionRecord) {
    final Object old = this.waitees.put(txnID, transactionRecord);
    Assert.assertNull(old);
  }

  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    synchronized (this.waitees) {
      TransactionRecord tr = this.waitees.get(txnID);
      if (tr == null) {
        tr = new TransactionRecord(true);
        createRecord(txnID, tr);
      }
    }
    addWaitee(to, txnID);
  }

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(final NodeID waitee, final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);

    if (transactionRecord == null) { return false; }
    synchronized (transactionRecord) {
      transactionRecord.remove(waitee);
      return checkCompletedAndRemove(txnID, transactionRecord);
    }
  }

  public void addWaitee(final NodeID waitee, final TransactionID txnID) {
    final TransactionRecord record = getRecord(txnID);
    synchronized (record) {
      final boolean added = record.addWaitee(waitee);
      Assert.eval(added);
    }
  }

  private TransactionRecord getRecord(final TransactionID txnID) {
    return this.waitees.get(txnID);
  }

  public boolean skipApplyAndCommit(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    synchronized (transactionRecord) {
      transactionRecord.applyAndCommitSkipped();
      return checkCompletedAndRemove(txnID, transactionRecord);
    }
  }

  public boolean applyCommitted(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    synchronized (transactionRecord) {
      transactionRecord.applyCommitted();
      return checkCompletedAndRemove(txnID, transactionRecord);
    }
  }

  public boolean broadcastCompleted(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    synchronized (transactionRecord) {
      transactionRecord.broadcastCompleted();
      return checkCompletedAndRemove(txnID, transactionRecord);
    }
  }

  public boolean processMetaDataCompleted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.processMetaDataCompleted();
      return checkCompletedAndRemove(requestID, transactionRecord);
    }
  }

  public boolean relayTransactionComplete(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    synchronized (transactionRecord) {
      transactionRecord.relayTransactionComplete();
      return checkCompletedAndRemove(txnID, transactionRecord);
    }
  }

  public boolean hasWaitees(final TransactionID txnID) {
    final TransactionRecord record = getRecord(txnID);
    if (record == null) { return false; }
    synchronized (record) {
      return !record.isEmpty();
    }
  }

  public Set requestersWaitingFor(final NodeID waitee) {
    final Set requesters = new HashSet();
    synchronized (this.waitees) {
      for (final Object element : this.waitees.entrySet()) {
        final Entry e = (Entry) element;
        final TransactionRecord record = (TransactionRecord) e.getValue();
        if (record.contains(waitee)) {
          final TransactionID requester = (TransactionID) e.getKey();
          requesters.add(requester);
        }
      }
    }
    return requesters;
  }

  private boolean checkCompletedAndRemove(final TransactionID txnID, final TransactionRecord record) {
    synchronized (this.waitees) {
      if (record.isComplete()) {
        this.waitees.remove(txnID);
        invokeCallBackOnCompleteIfNecessary();
        return !this.dead;
      }
      return false;
    }
  }

  public void addAllPendingServerTransactionIDsTo(final Set txnIDs) {
    synchronized (this.waitees) {
      for (final Object element : this.waitees.keySet()) {
        final TransactionID txnID = (TransactionID) element;
        txnIDs.add(new ServerTransactionID(this.sourceID, txnID));
      }
    }
  }

  public void nodeDead(final CallBackOnComplete cb) {
    synchronized (this.waitees) {
      this.callBack = cb;
      this.dead = true;
      invokeCallBackOnCompleteIfNecessary();
    }
  }

  private void invokeCallBackOnCompleteIfNecessary() {
    if (this.dead && this.waitees.isEmpty()) {
      this.callBack.onComplete(this.sourceID);
    }
  }

  @Override
  public String toString() {
    final StringBuilder strBuffer = new StringBuilder();
    strBuffer.append("TransactionAccount[" + this.sourceID + ": ");
    synchronized (this.waitees) {
      for (final Entry<TransactionID, TransactionRecord> entry : this.waitees.entrySet()) {
        strBuffer.append("{").append(entry.getKey()).append(": ");
        synchronized (entry.getValue()) {
          strBuffer.append(entry.getValue()).append("}\n\t");
        }
      }
    }
    return strBuffer.toString();
  }
}