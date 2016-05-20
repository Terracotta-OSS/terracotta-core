/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;


/**
 * This type is used by ActiveToPassiveReplication in order to wait on all the passives either sending a RECEIVED or
 * COMPLETED acknowledgement for a specific message.
 */
public class ActivePassiveAckWaiter {
  private final Set<NodeID> receivedPending;
  private final Set<NodeID> completedPending;

  public ActivePassiveAckWaiter(Set<NodeID> allPassiveNodes) {
    this.receivedPending =  new HashSet<NodeID>(allPassiveNodes);
    this.completedPending =  new HashSet<NodeID>(allPassiveNodes);
  }

  public synchronized void waitForReceived() throws InterruptedException {
    while (!this.receivedPending.isEmpty()) {
      wait();
    }
  }

  public synchronized void waitForCompleted() throws InterruptedException {
    while (!this.completedPending.isEmpty()) {
      wait();
    }
  }

  public synchronized boolean isCompleted() {
    return this.completedPending.isEmpty();
  }

  public synchronized void didReceiveOnPassive(NodeID onePassive) {
    boolean didContain = this.receivedPending.remove(onePassive);
    // We must have contained this passive in order to receive.
    Assert.assertTrue(didContain);
    // Wake everyone up if this changed something.
    if (this.receivedPending.isEmpty()) {
      notifyAll();
    }
  }

  /** 
   * @return True if this was the last outstanding completion required and the waiter is now done.
   */
  public synchronized boolean didCompleteOnPassive(NodeID onePassive) {
    // Note that we will try to remove from the received set, but usually it will already have been removed.
    boolean didContainInReceived = this.receivedPending.remove(onePassive);
    // We know that it must still be in the completed set, though.
    boolean didContainInCompleted = this.completedPending.remove(onePassive);
    // We must have contained this passive in order to complete.
    Assert.assertTrue(didContainInCompleted);
    boolean isDoneWaiting = this.completedPending.isEmpty();
    // Wake everyone up if this changed something.
    if ((didContainInReceived && this.receivedPending.isEmpty()) || isDoneWaiting) {
      notifyAll();
    }
    return isDoneWaiting;
  }
}
