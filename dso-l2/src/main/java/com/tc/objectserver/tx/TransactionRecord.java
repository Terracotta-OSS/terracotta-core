/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
    this(null);
  }

  public TransactionRecord(final NodeID node) {
    this.waitees = new HashSet<NodeID>();
    if (node != null) {
      this.state = TransactionState.COMPLETED_STATE;
      this.waitees.add(node);
    } else {
      this.state = new TransactionState();
    }
  }

  public synchronized boolean relayTransactionComplete() {
    this.state.relayTransactionComplete();
    notifyAll();
    return this.waitees.isEmpty() && this.state.isComplete();
  }

  public synchronized boolean applyAndCommitSkipped() {
    this.state.applyAndCommitSkipped();
    notifyAll();
    return this.waitees.isEmpty() && this.state.isComplete();
  }

  public synchronized boolean applyCommitted() {
    this.state.applyCommitted();
    notifyAll();
    return this.waitees.isEmpty() && this.state.isComplete();
  }

  public synchronized boolean broadcastCompleted() {
    this.state.broadcastCompleted();
    notifyAll();
    return this.waitees.isEmpty() && this.state.isComplete();
  }
  
  public synchronized boolean processMetaDataCompleted() {
    this.state.processMetaDataCompleted();
    notifyAll();
    return this.waitees.isEmpty() && this.state.isComplete();
  }

  public synchronized boolean isComplete() {
    return this.state.isComplete() && this.waitees.isEmpty();
  }

  @Override
  public String toString() {
    return "TransactionRecord@" + System.identityHashCode(this) + " = " + this.state + "  :: waitees = " + this.waitees;
  }

  public synchronized boolean addWaitee(final NodeID waitee) {
/* this the transaction is already complete, can't add waitee and switch state back to not completed  */
    if ( this.waitees.isEmpty() && this.state.isComplete() ) {
      return false;
    }
    return this.waitees.add(waitee);
  }

  public synchronized boolean remove(final NodeID waitee) {
    if (this.waitees.remove(waitee)) {
      notifyAll();
      if ( this.waitees.isEmpty() && this.state.isComplete() ) {
        return true;
      }
    }
    return false;
  }

  public synchronized boolean isEmpty() {
    return this.waitees.isEmpty();
  }

  public synchronized boolean contains(final NodeID waitee) {
    return this.waitees.contains(waitee);
  }

  private synchronized boolean isRelayComplete() {
    if (this.state.isRelayComplete()) {
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
    while (!this.state.isApplyCommitted()) {
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }
}
