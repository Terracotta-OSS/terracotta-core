/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;

import java.util.HashSet;
import java.util.Set;

public class TransactionRecord {
  private final TransactionState state;
  private final Set<NodeID>      waitees;

  public TransactionRecord() {
    this(false);
  }

  public TransactionRecord(final boolean objectSyncRecord) {
    this.waitees = new HashSet<NodeID>();
    if (objectSyncRecord) {
      this.state = TransactionState.COMPLETED_STATE;
    } else {
      this.state = new TransactionState();
    }
  }

  public void relayTransactionComplete() {
    this.state.relayTransactionComplete();
  }

  public void applyAndCommitSkipped() {
    this.state.applyAndCommitSkipped();
  }

  public void applyCommitted() {
    this.state.applyCommitted();
  }

  public void broadcastCompleted() {
    this.state.broadcastCompleted();
  }
  
  public void processMetaDataCompleted() {
    state.processMetaDataCompleted();
  }

  public boolean isComplete() {
    return this.state.isComplete() && this.waitees.isEmpty();
  }

  @Override
  public String toString() {
    return "TransactionRecord@" + System.identityHashCode(this) + " = " + this.state + "  :: waitees = " + this.waitees;
  }

  public boolean addWaitee(final NodeID waitee) {
    return this.waitees.add(waitee);
  }

  public boolean remove(final NodeID waitee) {
    return this.waitees.remove(waitee);
  }

  public boolean isEmpty() {
    return this.waitees.isEmpty();
  }

  public boolean contains(final NodeID waitee) {
    return this.waitees.contains(waitee);
  }

}
