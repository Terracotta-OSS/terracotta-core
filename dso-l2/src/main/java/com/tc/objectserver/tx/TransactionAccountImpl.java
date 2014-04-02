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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.ArrayList;
import java.util.Collection;

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

  @Override
  public NodeID getNodeID() {
    return this.sourceID;
  }

  @Override
  public void incomingTransactions(Set<ServerTransactionID> txnIDs) {
    Assert.assertFalse(this.dead);
    for (final ServerTransactionID stxnID : txnIDs) {
      createRecord(stxnID.getClientTransactionID(), new TransactionRecord());
    }
  }

  private void createRecord(final TransactionID txnID, final TransactionRecord transactionRecord) {
    final Object old = this.waitees.put(txnID, transactionRecord);
    Assert.assertNull(old);
  }

  @Override
  public void addObjectsSyncedTo(final NodeID to, final TransactionID txnID) {
    TransactionRecord tr = null;
    while ( tr == null ) {
      synchronized (this.waitees) {
        tr = this.waitees.get(txnID);
        if (tr == null) {
          tr = new TransactionRecord(to);
          createRecord(txnID, tr);
        } else if ( !tr.addWaitee(to) ) {
/*  couldn't add the waitee, try again  */
          tr = null;
        }
      }
    }
  }

  @Override
  public void waitUntilRelayed(final TransactionID txnID) {
    TransactionRecord record = getRecord(txnID);
    checkArgument(record != null, "%s not found.", txnID);
    record.waitUntilRelayComplete();
  }

  @Override
  public void waitUntilCommitted(final TransactionID txnID) {
    TransactionRecord record = getRecord(txnID);
    checkArgument(record != null, "%s not found.", txnID);
    record.waitUntilCommit();
  }

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  @Override
  public boolean removeWaitee(final NodeID waitee, final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);

    if (transactionRecord == null) { return false; }
    if ( transactionRecord.remove(waitee) ) {
      return removeTransaction(txnID);
    } else {
      return false;
    }
  }

  @Override
  public void addWaitee(final NodeID waitee, final TransactionID txnID) {
    final TransactionRecord record = getRecord(txnID);
    Assert.assertNotNull(record);
    final boolean added = record.addWaitee(waitee);
    Assert.eval(added);
  }
  
  private synchronized Collection<TransactionID> getTransactions() {
    return new ArrayList<TransactionID>(this.waitees.keySet());
  }

  private TransactionRecord getRecord(final TransactionID txnID) {
    return this.waitees.get(txnID);
  }

  @Override
  public boolean skipApplyAndCommit(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    if ( transactionRecord.applyAndCommitSkipped() ) {
      return removeTransaction(txnID);
    } else {
      return false;
    }
  }

  @Override
  public boolean applyCommitted(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    if ( transactionRecord.applyCommitted() ) {
      return removeTransaction(txnID);
    } else {
        return false;
    }
  }

  @Override
  public boolean broadcastCompleted(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    if ( transactionRecord.broadcastCompleted() ) {
      return removeTransaction(txnID);
    } else {
        return false;
    }
  }

  @Override
  public boolean processMetaDataCompleted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);
    if ( transactionRecord.processMetaDataCompleted() ) {
      return removeTransaction(requestID);
    } else {
      return false;
    }
  }

  @Override
  public boolean relayTransactionComplete(final TransactionID txnID) {
    final TransactionRecord transactionRecord = getRecord(txnID);
    if ( transactionRecord.relayTransactionComplete()) {
      return removeTransaction(txnID);
    } else {
      return false;
    }
  }

  @Override
  public boolean hasWaitees(final TransactionID txnID) {
    final TransactionRecord record = getRecord(txnID);
    if (record == null) { return false; }
    return !record.isEmpty();
  }

  @Override
  public Set requestersWaitingFor(final NodeID waitee) {
    final Set requesters = new HashSet();
    for (final TransactionID tid : getTransactions()) {
      final TransactionRecord record = (TransactionRecord) this.waitees.get(tid);
      if (record != null && record.contains(waitee)) {
        requesters.add(tid);
      }
    }
    return requesters;
  }

  private boolean removeTransaction(final TransactionID txnID) {
    synchronized (this.waitees) {
      this.waitees.remove(txnID);
      invokeCallBackOnCompleteIfNecessary();
      return !this.dead;
    }
  }

  @Override
  public void addAllPendingServerTransactionIDsTo(final Set txnIDs) {
    synchronized (this.waitees) {
      for (final Object element : this.waitees.keySet()) {
        final TransactionID txnID = (TransactionID) element;
        txnIDs.add(new ServerTransactionID(this.sourceID, txnID));
      }
    }
  }

  @Override
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