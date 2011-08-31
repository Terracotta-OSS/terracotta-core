/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.Sink;
import com.tc.async.impl.MockSink;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.objectserver.locks.factory.NonGreedyLockPolicyFactory;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.SinglyLinkedList.SinglyLinkedListIterator;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

public class LockTest extends TestCase {

  private Sink            sink;
  private long            uniqueId = 100000L;
  private LockManagerImpl lockMgr;
  private NotifiedWaiters notifiedWaiters;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.notifiedWaiters = new NotifiedWaiters();
    this.sink = new MockSink();
    lockMgr = new LockManagerImpl(this.sink, new NullChannelManager(), new NonGreedyLockPolicyFactory());
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter lockRecallCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter lockCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    DSOGlobalServerStatsImpl serverStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null,
                                                                        lockRecallCounter, null, null, lockCounter);
    L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER.start(new NullChannelManager(), serverStats,
                                                               ObjectStatsManager.NULL_OBJECT_STATS_MANAGER);
  }

  public void testLockClear() {
    // XXX: test the return value of lock.nextPending()
    // XXX: add this check into the other tests too

    // throw new ImplementMe();
  }

  public void testUpgrade() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    ThreadID thread2 = new ThreadID(2);
    ThreadID thread3 = new ThreadID(3);
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));
    lock.lock(cid1, thread1, ServerLockLevel.READ, helper);
    lock.lock(cid1, thread2, ServerLockLevel.READ, helper);
    lock.lock(cid1, thread3, ServerLockLevel.READ, helper);
    assertEquals(3, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    // try requesting the upgrade
    try {
      lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
      throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      // expected
    }
    assertEquals(3, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    lock.unlock(cid1, thread2, helper);
    assertEquals(2, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    // release 1 read lock
    lock.unlock(cid1, thread3, helper);

    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    ServerLockContext context = lock.getFirst();
    assertEquals(State.HOLDER_READ, context.getState());
    assertEquals(cid1, context.getClientID());
    assertEquals(thread1, context.getThreadID());
    // assertFalse(holder.isUpgrade());

    // add some other pending lock requests
    lock.lock(cid1, thread2, ServerLockLevel.READ, helper);
    lock.lock(cid1, thread3, ServerLockLevel.WRITE, helper);
    assertEquals(1, lock.getNoOfPendingRequests());
    assertEquals(2, getHoldersCount(lock));

    // release all reads
    lock.unlock(cid1, thread1, helper);
    lock.unlock(cid1, thread2, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    context = lock.getFirst();
    assertEquals(State.HOLDER_WRITE, context.getState());
    assertEquals(cid1, context.getClientID());
    assertEquals(thread3, context.getThreadID());

    // release the write lock
    lock.unlock(cid1, thread3, helper);
    assertEquals(0, lock.getNoOfPendingRequests());
    assertEquals(0, getHoldersCount(lock));
  }

  public void testMonitorStateAssertions() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));

    lock.wait(cid1, thread1, -1, helper); // indefinite

    // wait()
    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));

    try {
      lock.wait(cid1, thread1, -1, helper);
      fail("able to join wait set twice");
    } catch (TCIllegalMonitorStateException e) {
      // exptected
    }

    try {
      lock.notify(cid1, thread1, NotifyAction.ONE, notifiedWaiters, helper);
      fail("able to call notify whilst being in the wait set");
    } catch (TCIllegalMonitorStateException e) {
      // exptected
    }
  }

  public void testIllegalMonitorState() {
    ClientID goodcid = new ClientID(1);
    ThreadID goodtid = new ThreadID(1);

    ClientID badcid = new ClientID(2);
    ThreadID badtid = new ThreadID(2);
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(goodcid, goodtid, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertFalse(lock.hasPendingRequests());

    try {
      // different lock owner should not be to do a wait()
      lock.wait(badcid, badtid, -1, helper);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // different lock owner should not be to do a notify()
        lock.notify(badcid, badtid, NotifyAction.ONE, notifiedWaiters, helper);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // different lock owner should not be to do a notifyAll()
          lock.notify(badcid, badtid, NotifyAction.ALL, notifiedWaiters, helper);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }

    // make it so no one is holding .. since this is a greedy holder
    lock.unlock(goodcid, goodtid, helper);
    assertEquals(0, getHoldersCount(lock));

    try {
      // should not be able to wait() if no one holds a lock
      lock.wait(goodcid, goodtid, -1, helper);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // should not be able to notify() if no one holds a lock
        lock.notify(goodcid, goodtid, NotifyAction.ALL, notifiedWaiters, helper);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // should not be able to notifyAll() if no one holds a lock
          lock.notify(goodcid, goodtid, NotifyAction.ALL, notifiedWaiters, helper);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }

    // award a read lock
    assertEquals(0, getHoldersCount(lock));
    lock.lock(goodcid, goodtid, ServerLockLevel.READ, helper);
    assertEquals(1, getHoldersCount(lock));

    try {
      // should not be able to wait() if not holding a write lock
      lock.wait(goodcid, goodtid, -1, helper);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // should not be able to notify() if not holding a write lock
        lock.notify(goodcid, goodtid, NotifyAction.ONE, notifiedWaiters, helper);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // should not be able to notifyAll() if not holding a write lock
          lock.notify(goodcid, goodtid, NotifyAction.ALL, notifiedWaiters, helper);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }
  }

  public void testTimedWaitWithNotify() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    ThreadID thread2 = new ThreadID(2);
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);

    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.wait(cid1, thread1, 200, helper);
    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.lock(cid1, thread2, ServerLockLevel.WRITE, helper);
    lock.notify(cid1, thread2, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(1, lock.getNoOfPendingRequests());

    // give the timer a chance to run (even though it should be cancelled)
    ThreadUtil.reallySleep(2000);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(1, lock.getNoOfPendingRequests());
  }

  public void testTimedWait2() throws Exception {
    // excercise the 2 arg version of wait
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    lockMgr.start();

    LockHelper helper = lockMgr.getHelper();
    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    long t1 = System.currentTimeMillis();
    lock.wait(cid1, thread1, 2000, helper);
    helper.getLockStore().checkIn(lock);

    // This is still not perfect - This can only raise false negatives
    // assertEquals(0, getHoldersCount(lock));
    // assertEquals(1, getWaitersCount(lock));
    // assertFalse(lock.hasPending());

    while (getHoldersCount(lock) != 1) {
      ThreadUtil.reallySleep(100);
    }
    long t2 = System.currentTimeMillis();
    assertTrue(t2 - t1 >= 2000);

    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());
  }

  public void testTimedWaitsDontFireWhenLockManagerIsStopped() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    lockMgr.start();
    LockHelper helper = lockMgr.getHelper();

    // Test that a wait() timeout will obtain an uncontended lock
    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.wait(cid1, thread1, 1000, helper);
    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    ThreadUtil.reallySleep(250);
    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lockMgr.clearAllLocksFor(cid1);
  }

  public void testTimedWaits() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    ThreadID thread2 = new ThreadID(2);
    LockHelper helper = lockMgr.getHelper();

    lockMgr.start();
    {
      // Test that a wait() timeout will obtain an uncontended lock
      AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

      lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
      assertEquals(1, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertFalse(lock.hasPendingRequests());

      long t1 = System.currentTimeMillis();
      lock.wait(cid1, thread1, 1000, helper);
      // This is still not perfect - This can only raise false negatives
      // assertEquals(0, getHoldersCount(lock));
      // assertEquals(1, getWaitersCount(lock));
      // assertFalse(lock.hasPending());

      helper.getLockStore().checkIn(lock);

      while (getHoldersCount(lock) != 1) {
        ThreadUtil.reallySleep(100);
      }
      long t2 = System.currentTimeMillis();
      assertTrue(t2 - t1 >= 1000);

      assertEquals(1, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertFalse(lock.hasPendingRequests());

      lockMgr.clearAllLocksFor(cid1);
    }

    {
      // this time the wait timeout will cause the waiter to be put in the
      // pending
      // list (instead of instantly getting the lock)
      AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

      lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
      assertEquals(1, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertFalse(lock.hasPendingRequests());

      lock.wait(cid1, thread1, 500, helper);

      assertEquals(0, getHoldersCount(lock));
      assertEquals(1, getWaitersCount(lock));

      assertFalse(lock.hasPendingRequests());

      lock.lock(cid1, thread2, ServerLockLevel.WRITE, helper);
      assertEquals(1, getHoldersCount(lock));
      assertEquals(1, getWaitersCount(lock));
      assertFalse(lock.hasPendingRequests());

      helper.getLockStore().checkIn(lock);
      ThreadUtil.reallySleep(1000);
      assertEquals(1, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertEquals(1, lock.getNoOfPendingRequests());

      lock.unlock(cid1, thread2, helper);
      assertEquals(1, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertEquals(0, lock.getNoOfPendingRequests());

      helper.getLockStore().checkOut(lock.getLockID());
      lock.unlock(cid1, thread1, helper);
      assertEquals(0, getHoldersCount(lock));
      assertEquals(0, getWaitersCount(lock));
      assertEquals(0, lock.getNoOfPendingRequests());
    }
  }

  public void testWait() throws Exception {
    ClientID cid1 = new ClientID(1);
    ThreadID thread1 = new ThreadID(1);
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.wait(cid1, thread1, -1, helper);

    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());
  }

  public void testWaitOnAndNotify() throws Exception {
    ClientID cid1 = new ClientID(1);
    LockHelper helper = lockMgr.getHelper();

    ThreadID thread1 = new ThreadID(1);
    ThreadID thread2 = new ThreadID(2);

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertNull(lock.getNextRequestIfCanAward(helper));

    lock.wait(cid1, thread1, -1, helper);
    assertEquals(0, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    // make sure another thread can notify()
    lock.lock(cid1, thread2, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.notify(cid1, thread2, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(1, lock.getNoOfPendingRequests());

    lock.unlock(cid1, thread2, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    // make sure the wait()'er gets an write lock back
    assertEquals(1, getHoldersCount(lock));
    ServerLockContext holder = lock.getFirst();

    assertTrue(holder.isHolder());
    assertEquals(thread1, holder.getThreadID());
    // assertFalse(holder.isUpgrade());
    assertEquals(ServerLockLevel.WRITE, holder.getState().getLockLevel());
  }

  public void testNotifyAll() throws Exception {
    LockHelper helper = lockMgr.getHelper();
    AbstractServerLock lock = createLockWithIndefiniteWaits(100);

    assertEquals(0, getHoldersCount(lock));
    assertEquals(100, getWaitersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    ClientID notifierCid = getUniqueClientID();
    ThreadID notifierTid = getUniqueTransactionID();

    lock.lock(notifierCid, notifierTid, ServerLockLevel.WRITE, helper);
    lock.notify(notifierCid, notifierTid, NotifyAction.ALL, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(100, lock.getNoOfPendingRequests());

    lock.unlock(notifierCid, notifierTid, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(99, lock.getNoOfPendingRequests());
  }

  public void testNotify() throws Exception {
    LockHelper helper = lockMgr.getHelper();
    AbstractServerLock lock = createLockWithIndefiniteWaits(3);

    assertEquals(0, getHoldersCount(lock));
    assertEquals(3, getWaitersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    ClientID notifierCid = getUniqueClientID();
    ThreadID notifierTid = getUniqueTransactionID();
    lock.lock(notifierCid, notifierTid, ServerLockLevel.WRITE, helper);
    lock.notify(notifierCid, notifierTid, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(2, getWaitersCount(lock));
    assertEquals(1, lock.getNoOfPendingRequests());

    lock.notify(notifierCid, notifierTid, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(1, getWaitersCount(lock));
    assertEquals(2, lock.getNoOfPendingRequests());

    lock.notify(notifierCid, notifierTid, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(3, lock.getNoOfPendingRequests());

    // one more time, should have no effect
    lock.notify(notifierCid, notifierTid, NotifyAction.ONE, notifiedWaiters, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(3, lock.getNoOfPendingRequests());

    lock.unlock(notifierCid, notifierTid, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertEquals(2, lock.getNoOfPendingRequests());
  }

  public void testTimedTryLockAndCrashClient() throws Exception {
    ClientID cid1 = new ClientID(1);
    ClientID cid2 = new ClientID(2);
    ThreadID thread1 = new ThreadID(1);
    lockMgr.start();
    LockHelper helper = lockMgr.getHelper();

    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));

    lock.lock(cid1, thread1, ServerLockLevel.WRITE, helper);
    assertEquals(1, getHoldersCount(lock));
    assertEquals(0, getWaitersCount(lock));
    assertFalse(lock.hasPendingRequests());

    lock.tryLock(cid2, thread1, ServerLockLevel.WRITE, 10000, helper);
    assertEquals(1, getHoldersCount(lock));
    assertTrue(lock.hasPendingRequests());

    // DEV-3671: clearing of the locks should not throw any exception
    lockMgr.clearAllLocksFor(cid2);
    lockMgr.clearAllLocksFor(cid1);
  }

  private ClientID getUniqueClientID() {
    return new ClientID(uniqueId++);
  }

  private ThreadID getUniqueTransactionID() {
    return new ThreadID(uniqueId++);
  }

  private AbstractServerLock createLockWithIndefiniteWaits(int numWaits) throws TCIllegalMonitorStateException {
    LockHelper helper = lockMgr.getHelper();
    AbstractServerLock lock = (AbstractServerLock) helper.getLockStore().checkOut(new StringLockID("timmy"));
    for (int i = 0; i < numWaits; i++) {
      int before = getWaitersCount(lock);

      ClientID cid = getUniqueClientID();
      ThreadID tid = getUniqueTransactionID();
      lock.lock(cid, tid, ServerLockLevel.WRITE, helper);
      lock.wait(cid, tid, -1, helper);
      assertEquals(before + 1, getWaitersCount(lock));
    }

    assertEquals(numWaits, getWaitersCount(lock));
    assertEquals(0, getHoldersCount(lock));
    assertEquals(0, lock.getNoOfPendingRequests());

    return lock;
  }

  private int getHoldersCount(AbstractServerLock lock) {
    int count = 0;
    SinglyLinkedListIterator<ServerLockContext> iterator = lock.iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case HOLDER:
        case GREEDY_HOLDER:
          count++;
          break;
        default:
          return count;
      }
    }
    return count;
  }

  private int getWaitersCount(AbstractServerLock lock) {
    int count = 0;
    SinglyLinkedListIterator<ServerLockContext> iterator = lock.iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case WAITER:
          count++;
          break;
        default:
      }
    }
    return count;
  }
}
