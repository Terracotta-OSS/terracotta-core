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

import com.tc.l2.msg.ReplicationResultCode;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.mockito.Mockito.mock;


public class ActivePassiveAckWaiterTest {
  /**
   * Test that we can handle the degenerate case of an empty waiter.
   */
  @Test
  public void testEmptyWait() throws Exception {
    Set<SessionID> passives = Collections.emptySet();
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(Collections.emptyMap(), passives, null);
    // This can run in a single thread.
    Assert.assertTrue(waiter.isCompleted());
    waiter.waitForReceived();
    waiter.waitForCompleted();
    Assert.assertTrue(waiter.isCompleted());
  }

  @Test
  public void testSingleWait() throws Exception {
    Map<NodeID, SessionID> map = new HashMap<>();
    Set<SessionID> passives = new HashSet<>();
    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    NodeID node = mock(NodeID.class);
    map.put(node, onePassive);
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(1);
    LockStep lockStep = new LockStep(waiter, interlock);
    lockStep.start();
    interlock.waitOnStarts();
    waiter.didReceiveOnPassive(node);
    interlock.waitOnReceivesOnly();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    interlock.waitOnCompletes();
    lockStep.join();
  }

  @Test
  public void testMultiWait() throws Exception {
    Map<NodeID, SessionID> map = new HashMap<>();
    Set<SessionID> passives = new HashSet<>();

    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    SessionID twoPassive = mock(SessionID.class);
    passives.add(twoPassive);
    
    NodeID node1 = mock(NodeID.class);
    NodeID node2 = mock(NodeID.class);
    map.put(node1, onePassive);
    map.put(node2, twoPassive);
    
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(1);
    LockStep lockStep = new LockStep(waiter, interlock);
    lockStep.start();
    interlock.waitOnStarts();
    waiter.didReceiveOnPassive(node1);
    waiter.didReceiveOnPassive(node2);
    interlock.waitOnReceivesOnly();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node2, true, ReplicationResultCode.SUCCESS);
    Assert.assertFalse(waiterIsDone);
    waiterIsDone = waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    interlock.waitOnCompletes();
    lockStep.join();
  }

  @Test
  public void testMultiWaitWithMoreThreads() throws Exception {
    Set<SessionID> passives = new HashSet<>();
    Map<NodeID, SessionID> map = new HashMap<>();
    
    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    SessionID twoPassive = mock(SessionID.class);
    passives.add(twoPassive);
    
    NodeID node1 = mock(NodeID.class);
    NodeID node2 = mock(NodeID.class);
    
    map.put(node1, onePassive);
    map.put(node2, twoPassive);
    
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(2);
    LockStep lockStep1 = new LockStep(waiter, interlock);
    LockStep lockStep2 = new LockStep(waiter, interlock);
    lockStep1.start();
    lockStep2.start();
    interlock.waitOnStarts();
    waiter.didReceiveOnPassive(node1);
    waiter.didReceiveOnPassive(node2);
    interlock.waitOnReceivesOnly();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node2, true, ReplicationResultCode.SUCCESS);
    Assert.assertFalse(waiterIsDone);
    waiterIsDone = waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    interlock.waitOnCompletes();
    lockStep1.join();
    lockStep2.join();
  }

  @Test
  public void testImplicitReceive() throws Exception {
    Map<NodeID, SessionID> map = new HashMap<>();
    Set<SessionID> passives = new HashSet<>();
    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    SessionID twoPassive = mock(SessionID.class);
    passives.add(twoPassive);
    
    NodeID node1 = mock(NodeID.class);
    NodeID node2 = mock(NodeID.class);
    map.put(node1, onePassive);
    map.put(node2, twoPassive);
    
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(1);
    LockStep lockStep = new LockStep(waiter, interlock);
    lockStep.start();
    interlock.waitOnStarts();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node2, true, ReplicationResultCode.SUCCESS);
    Assert.assertFalse(waiterIsDone);
    waiterIsDone = waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    interlock.waitOnCompletes();
    lockStep.join();
  }

  @Test
  public void testFailedDoubleComplete() throws Exception {
    Map<NodeID, SessionID> map = new HashMap<>();
    Set<SessionID> passives = new HashSet<>();
    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    NodeID node1 = mock(NodeID.class);
    map.put(node1, onePassive);
    
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(1);
    LockStep lockStep = new LockStep(waiter, interlock);
    lockStep.start();
    interlock.waitOnStarts();
    waiter.didReceiveOnPassive(node1);
    interlock.waitOnReceivesOnly();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    boolean didFail = false;
    try {
      waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    } catch (AssertionError e) {
      // We expect this to fail on double-complete.
      didFail = true;
    }
    Assert.assertTrue(didFail);
    interlock.waitOnCompletes();
    lockStep.join();
  }

  @Test
  public void testDisconnectAfterComplete() throws Exception {
    Map<NodeID, SessionID> map = new HashMap<>();
    Set<SessionID> passives = new HashSet<>();
    SessionID onePassive = mock(SessionID.class);
    passives.add(onePassive);
    
    NodeID node1 = mock(NodeID.class);
    map.put(node1, onePassive);
    
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(map, passives, null);
    Interlock interlock = new Interlock(1);
    LockStep lockStep = new LockStep(waiter, interlock);
    lockStep.start();
    interlock.waitOnStarts();
    waiter.didReceiveOnPassive(node1);
    interlock.waitOnReceivesOnly();
    boolean waiterIsDone = waiter.didCompleteOnPassive(node1, true, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    // We will try to complete, again, but this won't assert, since we are stating that it is not a normal complete.
    waiterIsDone = waiter.didCompleteOnPassive(node1, false, ReplicationResultCode.SUCCESS);
    Assert.assertTrue(waiterIsDone);
    interlock.waitOnCompletes();
    lockStep.join();
  }


  private static class LockStep extends Thread {
    private final ActivePassiveAckWaiter waiter;
    private final Interlock interlock;
    
    public LockStep(ActivePassiveAckWaiter waiter, Interlock interlock) {
      this.waiter = waiter;
      this.interlock = interlock;
    }
    @Override
    public void run() {
      Assert.assertFalse(this.waiter.isCompleted());
      this.interlock.decrementStarts();
      this.waiter.waitForReceived();
      this.interlock.decrementReceives();
      this.waiter.waitForCompleted();
      this.interlock.decrementCompletes();
      Assert.assertTrue(this.waiter.isCompleted());
    }
  }


  private static class Interlock {
    private final int observerThreadCount;
    private int pendingStarts;
    private int pendingReceives;
    private int pendingCompletes;
    
    public Interlock(int observerThreadCount) {
      this.observerThreadCount = observerThreadCount;
      this.pendingStarts = observerThreadCount;
      this.pendingReceives = observerThreadCount;
      this.pendingCompletes = observerThreadCount;
    }
    
    public synchronized void decrementStarts() {
      this.pendingStarts -= 1;
      if (0 == this.pendingStarts) {
        notifyAll();
      }
    }
    
    public synchronized void decrementReceives() {
      this.pendingReceives -= 1;
      Assert.assertTrue(this.pendingReceives >= 0);
      if (0 == this.pendingReceives) {
        notifyAll();
      }
    }
    
    public synchronized void decrementCompletes() {
      this.pendingCompletes -= 1;
      Assert.assertTrue(this.pendingCompletes >= 0);
      if (0 == this.pendingCompletes) {
        notifyAll();
      }
    }
    
    public synchronized void waitOnStarts() throws InterruptedException {
      while (this.pendingStarts > 0) {
        wait();
      }
    }
    
    public synchronized void waitOnReceivesOnly() throws InterruptedException {
      while (this.pendingReceives > 0) {
        Assert.assertTrue(this.observerThreadCount == this.pendingCompletes);
        wait();
      }
      Assert.assertTrue(this.observerThreadCount == this.pendingCompletes);
    }
    
    public synchronized void waitOnCompletes() throws InterruptedException {
      while (this.pendingCompletes > 0) {
        Assert.assertTrue(this.pendingCompletes >= this.pendingReceives);
        wait();
      }
      Assert.assertTrue(0 == this.pendingReceives);
    }
  }
}
