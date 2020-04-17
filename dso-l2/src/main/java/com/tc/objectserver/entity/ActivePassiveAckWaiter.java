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
 */
package com.tc.objectserver.entity;

import com.tc.exception.TCServerRestartException;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This type is used by ActiveToPassiveReplication in order to wait on all the passives either sending a RECEIVED or
 * COMPLETED acknowledgement for a specific message.
 */
public class ActivePassiveAckWaiter {
  private final Map<NodeID, SessionID> session;
  private final Set<SessionID> start;
  private final Set<SessionID> receivedPending;
  private final Set<SessionID> receivedByComplete;
  private final Set<SessionID> completedPending;
  private Runnable finalizer;
  private final Map<NodeID, ReplicationResultCode> results;
  private final PassiveReplicationBroker parent;

  public ActivePassiveAckWaiter(Map<NodeID, SessionID> map, Set<SessionID> allPassiveNodes, PassiveReplicationBroker parent) {
    this.session = map;
    this.start =  Collections.unmodifiableSet(allPassiveNodes);
    this.receivedPending =  new HashSet<>(allPassiveNodes);
    this.completedPending =  new HashSet<>(allPassiveNodes);
    this.receivedByComplete =  new HashSet<>();
    this.results = new HashMap<>();
    this.parent = parent;
  }

  public synchronized void waitForReceived() {
    try {
      while (!this.receivedPending.isEmpty()) {
        wait();
      }
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }
  
  public void runWhenCompleted(Runnable r) {
    Runnable runInPlace = null;
    synchronized (this) {
      if (finalizer != null) {
        finalizer = ()->{
          finalizer.run();
          r.run();
        };
      } else {
        finalizer = r;
      }

      if (this.completedPending.isEmpty()) {
        runInPlace = finalizer;
        finalizer = null;
      } 
    }
    if (runInPlace != null) {
      runInPlace.run();
    }
  }

  public synchronized void waitForCompleted() {
    try {
      while (!this.completedPending.isEmpty()) {
        wait();
     }
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }
  
  public boolean verifyLifecycleResult(boolean success) {
    if(results.entrySet().stream().anyMatch(e->e.getValue() == (success ? ReplicationResultCode.FAIL : ReplicationResultCode.SUCCESS))) {
      boolean zapped = false;
      for (Map.Entry<NodeID, ReplicationResultCode> r : results.entrySet()) {
        if (r.getValue() == ReplicationResultCode.FAIL) {
          parent.zapAndWait(r.getKey());
          zapped = true;
        }
      }
      if (!success) {
        throw new TCServerRestartException("inconsistent lifecycle");
      }
      return zapped;
    }
    return false;
  }

  public synchronized boolean isCompleted() {
    return this.completedPending.isEmpty();
  }
  
  private SessionID nodeToSession(NodeID node) {
    return this.session.get(node);
  }

  public synchronized void didReceiveOnPassive(NodeID onePassive) {
    boolean didContain = this.receivedPending.remove(nodeToSession(onePassive));
    // We must have contained this passive in order to receive.
    if (!didContain) {
      Assert.assertTrue(onePassive + " " + toString(), this.receivedByComplete.contains(nodeToSession(onePassive)));
    }    
    // Wake everyone up if this changed something.
    if (this.receivedPending.isEmpty()) {
      notifyAll();
    }
  }

  /**
   * Notifies the waiter that it is complete for the given node.
   * 
   * @param onePassive The passive which has completed the replicated message
   * @param payload
   * @return True if this was the last outstanding completion required and the waiter is now done.
   */
  public boolean didCompleteOnPassive(NodeID onePassive, ReplicationResultCode payload) {
    // do this first to prevent updating the map while it is being checked
    this.results.put(onePassive, payload);
    return runFinalizerOnComplete(updateCompletionFlags(nodeToSession(onePassive), true));
  }
  
  public boolean failedToSendToPassive(SessionID session) {
    return runFinalizerOnComplete(updateCompletionFlags(session, false));
  }
  
  private boolean runFinalizerOnComplete(boolean completed) {
    if (completed) {
      Runnable clear = clearFinalizer();
      if (clear != null) {
        clear.run();
      }
    }
    return completed;
  }
  
  private synchronized Runnable clearFinalizer() {
    Runnable f = finalizer;
    finalizer = null;
    return f;
  }
  
  private synchronized boolean updateCompletionFlags(SessionID onePassive, boolean isNormal) {
    // Note that we will try to remove from the received set, but usually it will already have been removed.
    boolean didContainInReceived = this.receivedPending.remove(onePassive);
    if (didContainInReceived) {
      this.receivedByComplete.add(onePassive);
    }
    // We know that it must still be in the completed set, though.
    boolean didContainInCompleted = this.completedPending.remove(onePassive);
    if (isNormal && !didContainInCompleted) {
      throw new AssertionError("was completed twice");
    }
    // We must have contained this passive in order to complete.
    boolean isDoneWaiting = this.completedPending.isEmpty();
    // Wake everyone up if this changed something.
    if ((didContainInReceived && this.receivedPending.isEmpty()) || isDoneWaiting) {
      notifyAll();
    }

    return isDoneWaiting;
  }

  @Override
  public String toString() {
    return "ActivePassiveAckWaiter{" + "start=" + start + ", receivedPending=" + receivedPending + ", receivedByComplete=" + receivedByComplete + ", completedPending=" + completedPending + ", results=" + results + '}';
  }
}
