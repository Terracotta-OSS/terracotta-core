/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockResponseContext;
import com.tc.objectserver.locks.NullChannelManager;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class GreedyLockManagerTest extends TestCase {
  private TestSink        sink;
  private LockManagerImpl lockManager;

  static final int        numLocks   = 100;
  static final int        numThreads = 30;

  // private final ServerThreadID[] txns = makeUniqueTxns(numThreads);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sink = new TestSink();
    resetLockManager();
  }

  private void resetLockManager() {
    resetLockManager(false);
  }

  private void resetLockManager(boolean start) {
    sink.clear();

    lockManager = new LockManagerImpl(sink, new NullChannelManager());
    if (start) {
      lockManager.start();
    }
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter lockRecallCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter lockCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    DSOGlobalServerStatsImpl serverStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null,
                                                                        lockRecallCounter, null, null, lockCounter);
    L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER.start(new NullChannelManager(), serverStats,
                                                               ObjectStatsManager.NULL_OBJECT_STATS_MANAGER);
  }

  @Override
  protected void tearDown() throws Exception {
    assertEquals(0, lockManager.getLockCount());
    super.tearDown();
  }

  public void testLockMBean() {
    // MessageChannel channel = new TestMessageChannel();
    ClientID cid1 = new ClientID(1);
    ClientID cid2 = new ClientID(2);
    ClientID cid3 = new ClientID(3);
    LockID lid1 = new StringLockID("1");
    LockID lid2 = new StringLockID("2");
    LockID lid3 = new StringLockID("3");
    ThreadID tid1 = new ThreadID(1);

    // lockManager = new LockManagerImpl(sink, L2LockStatsManager.NULL_LOCK_STATS_MANAGER, new MyChannelManager(cid1,
    // channel));
    lockManager.start();

    lockManager.lock(lid1, cid1, tid1, ServerLockLevel.WRITE); // hold greedy
    lockManager.lock(lid1, cid2, tid1, ServerLockLevel.WRITE); // pending

    lockManager.lock(lid2, cid1, tid1, ServerLockLevel.READ); // hold greedy
    lockManager.lock(lid2, cid2, tid1, ServerLockLevel.READ); // hold greedy
    lockManager.lock(lid2, cid3, tid1, ServerLockLevel.WRITE); // pending

    lockManager.lock(lid3, cid1, tid1, ServerLockLevel.WRITE); // hold greedy

    // TODO: this part to be done after the beans have been implemented

    // LockMBean[] lockBeans = lockManager.getAllLocks();
    // assertEquals(3, lockBeans.length);
    // sortLocksByID(lockBeans);
    //
    // LockMBean bean1 = lockBeans[0];
    // LockMBean bean2 = lockBeans[1];
    // LockMBean bean3 = lockBeans[2];
    // testSerialize(bean1);
    // testSerialize(bean2);
    // testSerialize(bean3);
    //
    // validateBean1(bean1, start);
    // validateBean2(bean2, start);
    // validateBean3(bean3, start, wait);

    System.out.println("Lock Count = " + lockManager.getLockCount());

    lockManager.clearAllLocksFor(cid1);
    lockManager.clearAllLocksFor(cid2);
    lockManager.clearAllLocksFor(cid3);

    System.out.println("Lock Count = " + lockManager.getLockCount());
  }

  public void testReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);

    try {
      assertEquals(0, lockManager.getLockCount());
      long waitTime = 1000;
      long t0 = System.currentTimeMillis();
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      contexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.WAITER, waitTime));
      contexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.WAITER, waitTime * 2));
      lockManager.reestablishState(cid1, contexts);
      lockManager.start();

      // Wait timeout
      LockResponseContext ctxt = (LockResponseContext) sink.take();
      assertTrue(System.currentTimeMillis() - t0 >= waitTime);
      assertTrue(ctxt.isLockWaitTimeout());
      assertResponseContext(lockID1, cid1, tx1, ServerLockLevel.WRITE, ctxt);

      // Award - but should not give it as Greedy
      LockResponseContext ctxt1 = (LockResponseContext) sink.take();
      LockResponseContext ctxt2 = (LockResponseContext) sink.take();
      assertTrue(System.currentTimeMillis() - t0 >= waitTime);
      assertTrue((ctxt1.isLockAward() && ctxt2.isLockWaitTimeout())
                 || (ctxt2.isLockAward() && ctxt1.isLockWaitTimeout()));

      if (ctxt1.isLockAward()) {
        assertAwardNotGreedy(ctxt1, lockID1, tx1);
      } else if (ctxt2.isLockAward()) {
        assertAwardNotGreedy(ctxt2, lockID1, tx1);
      }

      lockManager.unlock(lockID1, cid1, tx1);

      // Award - Greedy
      ctxt = (LockResponseContext) sink.take();
      assertAwardGreedy(ctxt, lockID1);

      assertTrue(sink.waitForAdd(waitTime * 3) == null);

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  private void assertAwardNotGreedy(LockResponseContext ctxt, LockID lockID1, ThreadID tx1) {
    assertTrue(ctxt != null);
    assertTrue(ctxt.isLockAward());
    assertTrue(ctxt.getThreadID().equals(tx1));
    assertTrue(ctxt.getLockID().equals(lockID1));
    assertTrue(ctxt.getThreadID() != ThreadID.VM_ID);
  }

  private void assertAwardGreedy(LockResponseContext ctxt, LockID lockID1) {
    assertTrue(ctxt != null);
    assertTrue(ctxt.isLockAward());
    assertTrue(ctxt.getThreadID().equals(ThreadID.VM_ID));
    assertTrue(ctxt.getLockID().equals(lockID1));

  }

  public void testReestablishLockAfterReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);
    try {
      assertEquals(0, lockManager.getLockCount());

      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      contexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.WAITER, -1));
      lockManager.reestablishState(cid1, contexts);

      assertEquals(1, lockManager.getLockCount());
      assertEquals(0, sink.getInternalQueue().size());

      // now try to award the lock to the same client-transaction
      try {
        ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
        lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE));
        lockManager.reestablishState(cid1, lockContexts);
        fail("Should have thrown an Exception here.");
      } catch (AssertionError e) {
        // expected
      }
      // now try to reestablish the same lock from a different transaction. It
      // sould succeed
      assertEquals(1, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_WRITE));
      lockManager.reestablishState(cid1, lockContexts);
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testWaitTimeoutsIgnoredDuringStartup() throws Exception {
    LockID lockID = new StringLockID("my lcok");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    try {
      long waitTime = 1000;
      ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID, cid1, tx1, State.WAITER, waitTime));
      lockManager.reestablishState(cid1, lockContexts);

      LockResponseContext ctxt = (LockResponseContext) sink.waitForAdd(waitTime * 2);
      assertNull(ctxt);

      lockManager.start();
      ctxt = (LockResponseContext) sink.waitForAdd(0);
      assertNotNull(ctxt);
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishReadLock() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);
    ThreadID tx3 = new ThreadID(3);

    try {
      assertEquals(0, lockManager.getLockCount());

      ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_READ));
      lockManager.reestablishState(cid1, lockContexts);

      assertEquals(1, lockManager.getLockCount());

      // now reestablish the same read lock in another transaction. It should
      // succeed.
      sink.clear();
      lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ));
      lockManager.reestablishState(cid1, lockContexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish the the same write lock. It should fail.
      sink.clear();
      try {
        lockContexts = new ArrayList<ClientServerExchangeLockContext>();
        lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx3, State.HOLDER_WRITE));
        lockManager.reestablishState(cid1, lockContexts);
        fail("Should have thrown a LockManagerError.");
      } catch (AssertionError e) {
        //
      }

    } finally {
      // this needs to be done for tearDown() to pass.
      lockManager = null;
      resetLockManager();
    }

    try {
      sink.clear();
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE));
      lockManager.reestablishState(cid1, lockContexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish a read lock. This should fail.
      sink.clear();
      try {
        lockContexts = new ArrayList<ClientServerExchangeLockContext>();
        lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ));
        lockManager.reestablishState(cid1, lockContexts);
        fail("Should have thrown a LockManagerError");
      } catch (Error e) {
        //
      }

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishWriteLock() throws Exception {

    LockID lockID1 = new StringLockID("my lock");
    LockID lockID2 = new StringLockID("my other lock");
    ClientID cid1 = new ClientID(1);
    ClientID cid2 = new ClientID(2);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);

    try {
      sink.clear();
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE));
      lockManager.reestablishState(cid1, lockContexts);

      assertEquals(1, lockManager.getLockCount());

      try {
        lockContexts = new ArrayList<ClientServerExchangeLockContext>();
        lockContexts.add(new ClientServerExchangeLockContext(lockID1, cid2, tx2, State.HOLDER_WRITE));
        lockManager.reestablishState(cid1, lockContexts);

        fail("Expected a LockManagerError!");
      } catch (AssertionError e) {
        //
      }

      // try to reestablish another lock. It should succeed.
      lockContexts = new ArrayList<ClientServerExchangeLockContext>();
      lockContexts.add(new ClientServerExchangeLockContext(lockID2, cid1, tx1, State.HOLDER_WRITE));
      lockManager.reestablishState(cid1, lockContexts);

      lockManager.start();
      // you shouldn't be able to call reestablishLock after the lock manager
      // has started.
      try {
        lockContexts = new ArrayList<ClientServerExchangeLockContext>();
        lockContexts.add(new ClientServerExchangeLockContext(lockID2, cid1, tx1, State.HOLDER_WRITE));
        lockManager.reestablishState(cid1, lockContexts);

        fail("Should have thrown a LockManagerError");
      } catch (Error e) {
        //
      }

    } finally {
      // this needs to be done for tearDown() to pass.
      lockManager = null;
      resetLockManager();
    }
  }

  // private void assertResponseSink(LockID lockID, ChannelID channel, TransactionID tx, int requestedLevel,
  // TestSink responseSink) {
  // assertEquals(1, responseSink.getInternalQueue().size());
  // LockResponseContext ctxt = (LockResponseContext) responseSink.getInternalQueue().get(0);
  // assertResponseContext(lockID, channel, tx, requestedLevel, ctxt);
  // }

  private void assertResponseContext(LockID lockID, NodeID nid, ThreadID tx1, ServerLockLevel requestedLevel,
                                     LockResponseContext ctxt) {
    assertEquals(lockID, ctxt.getLockID());
    assertEquals(nid, ctxt.getNodeID());
    assertEquals(tx1, ctxt.getThreadID());
    assertEquals(requestedLevel, ctxt.getLockLevel());
  }

  public void testOffStopsGrantingNewLocks() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);
    try {
      // Test that the normal case works as expected...
      lockManager.start();
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());
      assertAwardGreedy((LockResponseContext) queue.get(0), lockID);
      sink.clear();
      lockManager.unlock(lockID, cid, ThreadID.VM_ID);

      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());
      assertAwardGreedy((LockResponseContext) queue.get(0), lockID);
      sink.clear();
      lockManager.unlock(lockID, cid, ThreadID.VM_ID);

      // Call shutdown and make sure that the lock isn't granted via the
      // "lock" method
      queue.clear();
    } finally {
      lockManager.clearAllLocksFor(cid);
    }
  }

  public void testRequestDoesntGrantPendingLocks() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);

    try {
      lockManager.start();
      // now try stacking locks and make sure that calling unlock doesn't grant
      // the pending locks but instead a recall is issued
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, new ClientID(2), new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending but a recall should be issued.
      assertEquals(1, queue.size());
      LockResponseContext lrc = (LockResponseContext) sink.take();
      assertTrue(lrc.isLockRecall());
      assertEquals(lockID, lrc.getLockID());
      assertEquals(cid, lrc.getNodeID());
      assertEquals(ThreadID.VM_ID, lrc.getThreadID());
      assertEquals(ServerLockLevel.WRITE, lrc.getLockLevel());
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testUnlockIgnoredDuringShutdown() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);
    try {
      lockManager.start();
      // now try stacking locks and make sure that calling unlock doesn't grant
      // the pending locks but instead a recall is issued
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, new ClientID(2), new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending but a recall should be issued.
      assertEquals(1, queue.size());
      LockResponseContext lrc = (LockResponseContext) sink.take();
      assertTrue(lrc.isLockRecall());
      assertEquals(lockID, lrc.getLockID());
      assertEquals(cid, lrc.getNodeID());
      assertEquals(ThreadID.VM_ID, lrc.getThreadID());
      assertEquals(ServerLockLevel.WRITE, lrc.getLockLevel());

      assertEquals(0, queue.size());
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }
}
