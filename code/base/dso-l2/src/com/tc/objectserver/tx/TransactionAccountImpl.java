/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
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
 * 
 */
public class TransactionAccountImpl implements TransactionAccount {
  final ChannelID   clientID;
  private final Map waitees = Collections.synchronizedMap(new HashMap());

  public TransactionAccountImpl(ChannelID clientID) {
    this.clientID = clientID;
  }

  public String toString() {
    return "TransactionAccount[" + clientID + ": waitees=" + waitees + "]\n";
  }

  public ChannelID getClientID() {
    return clientID;
  }

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(ChannelID waitee, TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);

    if (transactionRecord == null) return false;
    synchronized (transactionRecord) {
      transactionRecord.remove(waitee);
      return checkCompleted(requestID, transactionRecord);
    }
  }

  public void addWaitee(ChannelID waitee, TransactionID requestID) {
    TransactionRecord record = getOrCreateRecord(requestID);
    synchronized (record) {
      boolean added = record.addWaitee(waitee);
      Assert.eval(added);
    }
  }

  private TransactionRecord getOrCreateRecord(TransactionID requestID) {
    synchronized (waitees) {
      TransactionRecord transactionRecord = getRecord(requestID);
      if (transactionRecord == null) {
        waitees.put(requestID, (transactionRecord = new TransactionRecord()));
      }
      return transactionRecord;
    }
  }

  private TransactionRecord getRecord(TransactionID requestID) {
    TransactionRecord transactionRecord = (TransactionRecord) waitees.get(requestID);
    return transactionRecord;
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    TransactionRecord transactionRecord = getOrCreateRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.applyAndCommitSkipped();
      return checkCompleted(requestID, transactionRecord);
    }
  }

  public void applyStarted(TransactionID requestID) {
    TransactionRecord transactionRecord = getOrCreateRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.applyStarted();
    }
  }

  public boolean applyCommitted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);// (TransactionRecord) waitees.get(requestID);
    if (transactionRecord == null) {
      // this can happen when a client crashes and there are still some unprocessed apply messages in the
      // queue. We dont want to try to send acks for such scenario.
      return false;
    }
    synchronized (transactionRecord) {
      transactionRecord.applyCommitted();
      return checkCompleted(requestID, transactionRecord);
    }
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    TransactionRecord transactionRecord = getRecord(requestID);// (TransactionRecord) waitees.get(requestID);
    if (transactionRecord == null) {
      // this could happen when a client crashes and there are still some unprocessed apply messages in the
      // queue. We dont want to try to send acks for such scenario.
      return false;
    }
    synchronized (transactionRecord) {
      transactionRecord.broadcastCompleted();
      return checkCompleted(requestID, transactionRecord);
    }
  }

  public boolean relayTransactionComplete(TransactionID requestID) {
    TransactionRecord transactionRecord = getOrCreateRecord(requestID);
    synchronized (transactionRecord) {
      transactionRecord.relayTransactionComplete();
      return checkCompleted(requestID, transactionRecord);
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
        // if (((Set) waitees.get(requester)).contains(waitee))
        if (getRecord(requester).contains(waitee)) {
          requesters.add(requester);
        }
      }
    }
    return requesters;
  }

  private boolean checkCompleted(TransactionID requestID, TransactionRecord record) {
    if (record.isComplete()) {
      waitees.remove(requestID);
      return true;
    }
    return false;
  }

}