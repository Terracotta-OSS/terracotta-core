/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

public interface TransactionAccount {

  public abstract ChannelID getClientID();

  /*
   * returns true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public abstract boolean removeWaitee(ChannelID waitee, TransactionID requestID);

  public abstract void addWaitee(ChannelID waitee, TransactionID requestID);

  public abstract boolean skipApplyAndCommit(TransactionID requestID);

  public abstract void applyStarted(TransactionID requestID);

  public abstract boolean applyCommitted(TransactionID requestID);

  public abstract boolean broadcastCompleted(TransactionID requestID);

  public abstract boolean hasWaitees(TransactionID requestID);

  public abstract Set requestersWaitingFor(ChannelID waitee);

  public abstract boolean relayTransactionComplete(TransactionID requestID);

}