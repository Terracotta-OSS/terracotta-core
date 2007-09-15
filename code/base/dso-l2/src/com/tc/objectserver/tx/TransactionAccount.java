/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;

import java.util.HashSet;
import java.util.Set;

public interface TransactionAccount {

  public ChannelID getClientID();

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(ChannelID waitee, TransactionID requestID);

  public void addWaitee(ChannelID waitee, TransactionID requestID);

  public boolean skipApplyAndCommit(TransactionID requestID);

  public boolean applyCommitted(TransactionID requestID);

  public boolean broadcastCompleted(TransactionID requestID);

  public boolean hasWaitees(TransactionID requestID);

  public Set requestersWaitingFor(ChannelID waitee);

  public boolean relayTransactionComplete(TransactionID requestID);

  public void incommingTransactions(Set serverTxnIDs);

  public void addAllPendingServerTransactionIDsTo(HashSet txnsInSystem);

  public void clientDead(CallBackOnComplete callBack);

  public interface CallBackOnComplete {
    public void onComplete(ChannelID dead);
  }

}