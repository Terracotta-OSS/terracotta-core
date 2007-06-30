/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PassiveTransactionAccount implements TransactionAccount {

  private final ChannelID clientID;
  private final Set txnIDs = Collections.synchronizedSet(new HashSet());

  public PassiveTransactionAccount(ChannelID clientID) {
    this.clientID = clientID;
  }

  public void addWaitee(ChannelID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  public boolean applyCommitted(TransactionID requestID) {
    txnIDs.remove(new ServerTransactionID(clientID,requestID));
    return true;
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    throw new AssertionError("Transactions should never be broadcasted in Passive Server");
  }

  public ChannelID getClientID() {
    return clientID;
  }

  public boolean hasWaitees(TransactionID requestID) {
    return false;
  }

  public boolean removeWaitee(ChannelID waitee, TransactionID requestID) {
    throw new AssertionError("Transactions should never be ACKED to Passive Server");
  }

  public Set requestersWaitingFor(ChannelID waitee) {
    return Collections.EMPTY_SET;
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    txnIDs.remove(new ServerTransactionID(clientID,requestID));
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
}
