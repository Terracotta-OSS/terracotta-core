/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;

import java.util.Collections;
import java.util.Set;

public class PassiveTransactionAccount implements TransactionAccount {

  private final ChannelID clientID;

  public PassiveTransactionAccount(ChannelID clientID) {
    this.clientID = clientID;
  }

  public void addWaitee(ChannelID waitee, TransactionID requestID) {
    // NOP
  }

  public boolean applyCommitted(TransactionID requestID) {
    return false;
  }

  public void applyStarted(TransactionID requestID) {
    // NOP
  }

  public boolean broadcastCompleted(TransactionID requestID) {
    return false;
  }

  public ChannelID getClientID() {
    return clientID;
  }

  public boolean hasWaitees(TransactionID requestID) {
    return false;
  }

  public boolean removeWaitee(ChannelID waitee, TransactionID requestID) {
    return false;
  }

  public Set requestersWaitingFor(ChannelID waitee) {
    return Collections.EMPTY_SET;
  }

  public boolean skipApplyAndCommit(TransactionID requestID) {
    return false;
  }

  public boolean relayTransactionComplete(TransactionID requestID) {
    return false;
  }
}
