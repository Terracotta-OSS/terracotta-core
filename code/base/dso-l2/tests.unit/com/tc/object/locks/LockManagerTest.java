/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockResponseContext;
import com.tc.objectserver.locks.NullChannelManager;
import com.tc.objectserver.locks.factory.NonGreedyLockPolicyFactory;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class LockManagerTest extends TestCase {
  private TestSink        sink;
  private LockManagerImpl lockManager;

  static final int        numLocks   = 30;
  static final int        numThreads = 15;

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
    lockManager = new LockManagerImpl(sink, new NullChannelManager(), new NonGreedyLockPolicyFactory());
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
    final ClientID cid1 = new ClientID(1);
    final ClientID cid2 = new ClientID(2);

    LockID lid1 = new StringLockID("1");
    LockID lid2 = new StringLockID("2");
    LockID lid3 = new StringLockID("3");
    ThreadID tid1 = new ThreadID(1);
    ThreadID tid2 = new ThreadID(2);
    L2LockStatsManager lockStatsManager = new L2LockStatisticsManagerImpl();
    lockManager = new LockManagerImpl(sink, new NullChannelManager() {
      @Override
      public String getChannelAddress(NodeID nid) {
        if (cid1.equals(nid)) { return "127.0.0.1:6969"; }
        return "no longer connected";
      }
    }, new NonGreedyLockPolicyFactory());
    lockManager.setLockStatisticsEnabled(true, lockStatsManager);

    lockManager.start();
    lockStatsManager.start(new NullChannelManager(), null, ObjectStatsManager.NULL_OBJECT_STATS_MANAGER);

    lockManager.lock(lid1, cid1, tid1, ServerLockLevel.WRITE); // hold
    lockManager.lock(lid1, cid2, tid2, ServerLockLevel.WRITE); // pending

    lockManager.lock(lid2, cid1, tid1, ServerLockLevel.READ); // hold
    lockManager.lock(lid2, cid2, tid2, ServerLockLevel.READ); // hold
    try {
      lockManager.lock(lid2, cid1, tid1, ServerLockLevel.WRITE); // try upgrade and fail
      throw new AssertionError("Should have thrown an TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      //
    }

    lockManager.lock(lid3, cid1, tid1, ServerLockLevel.WRITE); // hold
    lockManager.wait(lid3, cid1, tid1, Integer.MAX_VALUE); // wait

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

    lockManager.clearAllLocksFor(cid1);
    lockManager.clearAllLocksFor(cid2);
  }

  public void testReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID nid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);

    try {
      assertEquals(0, lockManager.getLockCount());
      long waitTime = 1000;
      long t0 = System.currentTimeMillis();

      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, nid1, tx1, State.WAITER,
                                                                                    waitTime);
      contexts.add(context);
      context = new ClientServerExchangeLockContext(lockID1, nid1, tx2, State.WAITER, waitTime * 2);
      contexts.add(context);
      lockManager.reestablishState(nid1, contexts);
      lockManager.start();

      LockResponseContext ctxt = (LockResponseContext) sink.waitForAdd(waitTime * 3);
      assertTrue(ctxt != null);
      assertTrue(System.currentTimeMillis() - t0 >= waitTime);
      assertResponseContext(lockID1, nid1, tx1, ServerLockLevel.WRITE, ctxt);
      assertTrue(ctxt.isLockWaitTimeout());
      ThreadUtil.reallySleep(waitTime * 3);
      assertEquals(3, sink.size()); // 2 wait timeouts and 1 award
      sink.take();
      LockResponseContext ctxt1 = (LockResponseContext) sink.take();
      LockResponseContext ctxt2 = (LockResponseContext) sink.take();
      assertTrue((ctxt1.isLockAward() && ctxt2.isLockWaitTimeout())
                 || (ctxt2.isLockAward() && ctxt1.isLockWaitTimeout()));

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishLockAfterReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);
    try {
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.WAITER,
                                                                                    -1);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());
      assertEquals(0, sink.getInternalQueue().size());

      // now try to award the lock to the same client-transaction
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
        fail("Should have thrown an AssertionError.");
      } catch (AssertionError e) {
        // expected
      }
      // now try to reestablish the same lock from a different transaction. It
      // sould succeed
      assertEquals(1, lockManager.getLockCount());
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
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

      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_READ);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish the same read lock in another transaction. It should
      // succeed.
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish the the same write lock. It should fail.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx3, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);

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
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish a read lock. This should fail.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
        fail("Should have thrown a LockManagerError");
      } catch (AssertionError e) {
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
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid2, tx2, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid2, contexts);
        fail("Expected a LockManagerError!");
      } catch (AssertionError e) {
        //
      }

      // try to reestablish another lock. It should succeed.
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID2, cid1, tx1, State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);

      lockManager.start();
      // you shouldn't be able to call reestablishLock after the lock manager
      // has started.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
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

  public void testWaitTimeoutsIgnoredDuringShutdown() {
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);

    lockManager.start();
    lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);

    lockManager.wait(lockID, cid, txID, 1000);
    lockManager.clearAllLocksFor(cid);
  }

  public void testOffCancelsWaits() throws Exception {
    // implement me.
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
      queue.clear();
      lockManager.unlock(lockID, cid, txID);
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());
      lockManager.unlock(lockID, cid, txID);

      // Call shutdown and make sure that the lock isn't granted via the
      // "requestLock" method
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
      // the pending locks.
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, cid, new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending.
      assertEquals(0, queue.size());
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
      // the pending locks.
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, cid, new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending.
      assertEquals(0, queue.size());
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testLockManagerBasics() {
    LockID l1 = new StringLockID("1");
    ClientID c1 = new ClientID(1);
    ThreadID s1 = new ThreadID(0);

    ClientID c2 = new ClientID(2);
    ClientID c3 = new ClientID(3);
    ClientID c4 = new ClientID(4);
    lockManager.start();
    lockManager.lock(l1, c1, s1, ServerLockLevel.WRITE);
    assertTrue(sink.size() == 1);
    System.out.println(sink.getInternalQueue().remove(0));

    lockManager.lock(l1, c2, s1, ServerLockLevel.WRITE);
    assertTrue(sink.size() == 0);
    lockManager.unlock(l1, c1, s1);
    assertTrue(sink.size() == 1);
    System.out.println(sink.getInternalQueue().remove(0));

    lockManager.lock(l1, c3, s1, ServerLockLevel.READ);
    assertTrue(sink.size() == 0);
    assertTrue(lockManager.hasPending(l1));
    lockManager.unlock(l1, c2, s1);
    assertTrue(sink.size() == 1);
    assertFalse(lockManager.hasPending(l1));

    lockManager.lock(l1, c4, s1, ServerLockLevel.WRITE);
    assertTrue(lockManager.hasPending(l1));
    lockManager.unlock(l1, c3, s1);
    assertFalse(lockManager.hasPending(l1));
    lockManager.unlock(l1, c4, s1);
  }

  public void disableTestUpgradeDeadLock() {
    // Detect deadlock in competing upgrades
    LockID l1 = new StringLockID("L1");

    ClientID c0 = new ClientID(0);
    ThreadID s1 = new ThreadID(1);
    ThreadID s2 = new ThreadID(2);

    lockManager.start();

    // thread1 gets lock1 (R)
    lockManager.lock(l1, c0, s1, ServerLockLevel.READ);
    // thread2 gets lock1 (R)
    lockManager.lock(l1, c0, s2, ServerLockLevel.READ);

    // thread1 requests upgrade
    lockManager.lock(l1, c0, s1, ServerLockLevel.WRITE);
    // thread2 requests upgrade
    lockManager.lock(l1, c0, s2, ServerLockLevel.WRITE);

    lockManager.clearAllLocksFor(c0);
  }
}
