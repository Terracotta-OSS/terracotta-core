/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.util.Util;

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

  public synchronized void relayTransactionComplete() {
    this.state.relayTransactionComplete();
    notifyAll();
  }

  public synchronized void applyAndCommitSkipped() {
    this.state.applyAndCommitSkipped();
    notifyAll();
  }

  public synchronized void applyCommitted() {
    this.state.applyCommitted();
    notifyAll();
  }

  public synchronized void broadcastCompleted() {
    this.state.broadcastCompleted();
    notifyAll();
  }
  
  public synchronized void processMetaDataCompleted() {
    state.processMetaDataCompleted();
    notifyAll();
  }

  public synchronized boolean isComplete() {
    return this.state.isComplete() && this.waitees.isEmpty();
  }

  @Override
  public String toString() {
    return "TransactionRecord@" + System.identityHashCode(this) + " = " + this.state + "  :: waitees = " + this.waitees;
  }

  public synchronized boolean addWaitee(final NodeID waitee) {
    return this.waitees.add(waitee);
  }

  public synchronized boolean remove(final NodeID waitee) {
    boolean removed = this.waitees.remove(waitee);
    notifyAll();
    return removed;
  }

  public synchronized boolean isEmpty() {
    return this.waitees.isEmpty();
  }

  public synchronized boolean contains(final NodeID waitee) {
    return this.waitees.contains(waitee);
  }

  private synchronized boolean isRelayComplete() {
    if (state.isRelayComplete()) {
      for (NodeID waitee : waitees) {
        if (waitee.getNodeType() == NodeID.SERVER_NODE_TYPE) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public synchronized void waitUntilRelayComplete() {
    boolean interrupted = false;
    while(!isRelayComplete()) {
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }

  public synchronized void waitUntilCommit() {
    boolean interrupted = false;
    while (!state.isApplyCommitted()) {
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }
}
