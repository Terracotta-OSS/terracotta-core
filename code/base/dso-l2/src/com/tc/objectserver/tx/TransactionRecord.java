/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;

import java.util.HashSet;
import java.util.Set;

public class TransactionRecord {
  private final TransactionState state;
  private final Set              waitees;

  public TransactionRecord() {
    this.state = new TransactionState();
    this.waitees = new HashSet();
  }

  public void applyAndCommitSkipped() {
    state.applyAndCommitSkipped();
  }

  public void applyStarted() {
    state.applyStarted();
  }

  public void applyCommitted() {
    state.applyCommitted();
  }

  public void broadcastCompleted() {
    state.broadcastCompleted();
  }

  public boolean isComplete() {
    return state.isComplete() && waitees.isEmpty();
  }

  public String toString() {
    return "TransactionRecord@" + System.identityHashCode(this) + " = " + state + "  :: waitees = " + waitees;
  }

  public boolean addWaitee(ChannelID waitee) {
    return waitees.add(waitee);
  }

  public boolean remove(ChannelID waitee) {
    return waitees.remove(waitee);
  }

  public boolean isEmpty() {
    return waitees.isEmpty();
  }

  public boolean contains(ChannelID waitee) {
    return waitees.contains(waitee);
  }
}
