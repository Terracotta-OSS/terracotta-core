/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.async.impl.MockSink;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.impl.TCLockTimerImpl;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.lockmanager.api.LockAwardContext;
import com.tc.objectserver.lockmanager.api.LockEventListener;
import com.tc.objectserver.lockmanager.api.LockEventMonitor;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.objectserver.lockmanager.api.LockEventMonitor.CallContext;
import com.tc.util.TCAssertionError;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class LockTest extends TestCase {

  private Sink            sink;
  private long            uniqueId = 100000L;
  private TCLockTimer       waitTimer;
  private LockManagerImpl lockMgr  = new LockManagerImpl(new NullChannelManager(), L2LockStatsManager.NULL_LOCK_STATS_MANAGER);
  private NotifiedWaiters notifiedWaiters;

  protected void setUp() throws Exception {
    super.setUp();
    this.notifiedWaiters = new NotifiedWaiters();
    this.sink = new MockSink();
    this.waitTimer = new TCLockTimerImpl();
  }

  public void testLockClear() {
    // XXX: test the return value of lock.nextPending()
    // XXX: add this check into the other tests too

    // throw new ImplementMe();
  }

  public void testUpgrade() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);
    ThreadID txnId3 = new ThreadID(3);

    ServerThreadContext thread1 = makeTxn(cid1, txnId1);
    ServerThreadContext thread2 = makeTxn(cid1, txnId2);
    ServerThreadContext thread3 = makeTxn(cid1, txnId3);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.READ, sink);
    lock.requestLock(thread2, LockLevel.READ, sink);
    lock.requestLock(thread3, LockLevel.READ, sink);
    assertEquals(3, lock.getHoldersCount());
    assertEquals(0, lock.getPendingCount());

    // try requesting the upgrade
    try {
      lock.requestLock(thread1, LockLevel.WRITE, sink);
      throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      // expected
    }
    assertEquals(3, lock.getHoldersCount());
    assertEquals(0, lock.getPendingCount());

    lock.removeCurrentHold(thread2);
    assertEquals(2, lock.getHoldersCount());
    assertEquals(0, lock.getPendingCount());

    // release 1 read lock
    lock.removeCurrentHold(thread3);

    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getPendingCount());
    Holder holder = (Holder) lock.getHoldersCollection().toArray()[0];
    assertEquals(cid1, holder.getNodeID());
    assertEquals(txnId1, holder.getThreadID());
    //assertFalse(holder.isUpgrade());

    // add some other pending lock requests
    lock.requestLock(thread2, LockLevel.READ, sink);
    lock.requestLock(thread3, LockLevel.WRITE, sink);
    assertEquals(1, lock.getPendingCount());
    assertEquals(2, lock.getHoldersCount());

    // release all reads
    lock.removeCurrentHold(thread1);
    lock.removeCurrentHold(thread2);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getPendingCount());
    Holder[] holders = (Holder[]) lock.getHoldersCollection().toArray(new Holder[] {});
    assertEquals(0, holders.length);

    // release one of the current read locks
    lock.removeCurrentHold(thread1);
    lock.nextPending();
    assertEquals(0, lock.getPendingCount());
    assertEquals(1, lock.getHoldersCount());
    holder = (Holder) lock.getHoldersCollection().toArray()[0];
    assertEquals(cid1, holder.getNodeID());
    assertEquals(txnId3, holder.getThreadID());
    assertEquals(LockLevel.WRITE, holder.getLockLevel());


    // release the write lock
    lock.removeCurrentHold(thread3);
    assertEquals(0, lock.getPendingCount());
    assertEquals(0, lock.getHoldersCount());
  }

  private static ServerThreadContext makeTxn(ClientID cid, ThreadID threadID) {
    return new ServerThreadContext(cid, threadID);
  }

  public void testMonitorStateAssertions() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);

    ServerThreadContext thread1 = makeTxn(cid1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    lock.wait(thread1, waitTimer, new TimerSpec(), lockMgr, sink); // indefinite
    // wait()
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());

    try {
      lock.wait(thread1, waitTimer, new TimerSpec(), lockMgr, sink);
      fail("able to join wait set twice");
    } catch (TCAssertionError e) {
      // exptected
    }

    try {
      lock.notify(thread1, false, notifiedWaiters);
      fail("able to call notify whilst being in the wait set");
    } catch (TCAssertionError e) {
      // exptected
    }
  }

  public void testIllegalMonitorState() {
    ClientID goodClientID = new ClientID(new ChannelID(1));
    ThreadID goodTxnId = new ThreadID(1);

    ClientID badDClientID = new ClientID(new ChannelID(2));
    ThreadID badTxnId = new ThreadID(2);

    ServerThreadContext good = makeTxn(goodClientID, goodTxnId);
    ServerThreadContext bad = makeTxn(badDClientID, badTxnId);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(good, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertFalse(lock.hasPending());

    try {
      // different lock owner should not be to do a wait()
      lock.wait(bad, waitTimer, new TimerSpec(), lockMgr, sink);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // different lock owner should not be to do a notify()
        lock.notify(bad, false, notifiedWaiters);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // different lock owner should not be to do a notifyAll()
          lock.notify(bad, true, notifiedWaiters);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }

    // make it so no one is holding
    lock.removeCurrentHold(good);
    lock.nextPending();
    assertEquals(0, lock.getHoldersCount());

    try {
      // should not be able to wait() if no one holds a lock
      lock.wait(good, waitTimer, new TimerSpec(), lockMgr, sink);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // should not be able to notify() if no one holds a lock
        lock.notify(good, false, notifiedWaiters);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // should not be able to notifyAll() if no one holds a lock
          lock.notify(good, true, notifiedWaiters);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }

    // award a read lock
    assertEquals(0, lock.getHoldersCount());
    lock.requestLock(good, LockLevel.READ, sink);
    assertEquals(1, lock.getHoldersCount());

    try {
      // should not be able to wait() if not holding a write lock
      lock.wait(good, waitTimer, new TimerSpec(), lockMgr, sink);
      fail("Not expected");
    } catch (TCIllegalMonitorStateException e) {
      try {
        // should not be able to notify() if not holding a write lock
        lock.notify(good, false, notifiedWaiters);
        fail("Not expected");
      } catch (TCIllegalMonitorStateException e2) {
        try {
          // should not be able to notifyAll() if not holding a write lock
          lock.notify(good, true, notifiedWaiters);
          fail("Not expected");
        } catch (TCIllegalMonitorStateException e3) {
          // expected
        }
      }
    }
  }

  public void testTimedWaitWithNotify() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);

    ServerThreadContext thread1 = makeTxn(cid1, txnId1);
    ServerThreadContext thread2 = makeTxn(cid1, txnId2);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);

    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new TimerSpec(200), lockMgr, sink);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.requestLock(thread2, LockLevel.WRITE, sink);
    lock.notify(thread2, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(1, lock.getPendingCount());

    // give the timer a chance to run (even though it should be cancelled)
    ThreadUtil.reallySleep(2000);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(1, lock.getPendingCount());
  }

  public void testTimedWait2() throws Exception {
    // excercise the 2 arg version of wait
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);
    lockMgr.start();

    ServerThreadContext thread1 = makeTxn(cid1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    long t1 = System.currentTimeMillis();
    lock.wait(thread1, waitTimer, new TimerSpec(2000, 500), lockMgr, sink);
    // This is still not perfect - This can only raise false negatives
    // assertEquals(0, lock.getHoldersCount());
    // assertEquals(1, lock.getWaiterCount());
    // assertFalse(lock.hasPending());

    while (lock.getHoldersCount() != 1) {
      ThreadUtil.reallySleep(100);
    }
    long t2 = System.currentTimeMillis();
    assertTrue(t2 - t1 >= 2000);

    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());
  }

  public void testTimedWaitsDontFireWhenLockManagerIsStopped() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);
    lockMgr.start();

    // Test that a wait() timeout will obtain an uncontended lock
    ServerThreadContext thread1 = makeTxn(cid1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new TimerSpec(1000), lockMgr, sink);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    ThreadUtil.reallySleep(250);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    // stop the lock manager
    lockMgr.stop();

    ThreadUtil.reallySleep(250);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    ThreadUtil.reallySleep(2000);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());
  }

  public void testTimedWaits() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);
    lockMgr.start();
    {
      // Test that a wait() timeout will obtain an uncontended lock
      ServerThreadContext thread1 = makeTxn(cid1, txnId1);

      Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
      lock.requestLock(thread1, LockLevel.WRITE, sink);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      long t1 = System.currentTimeMillis();
      lock.wait(thread1, waitTimer, new TimerSpec(1000), lockMgr, sink);
      // This is still not perfect - This can only raise false negatives
      // assertEquals(0, lock.getHoldersCount());
      // assertEquals(1, lock.getWaiterCount());
      // assertFalse(lock.hasPending());

      while (lock.getHoldersCount() != 1) {
        ThreadUtil.reallySleep(100);
      }
      long t2 = System.currentTimeMillis();
      assertTrue(t2 - t1 >= 1000);

      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());

    }

    {
      // this time the wait timeout will cause the waiter to be put in the
      // pending
      // list (instead of instantly getting the lock)
      ServerThreadContext thread1 = makeTxn(cid1, txnId1);
      ServerThreadContext thread2 = makeTxn(cid1, txnId2);
      Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
      lock.requestLock(thread1, LockLevel.WRITE, sink);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      lock.wait(thread1, waitTimer, new TimerSpec(500), lockMgr, sink);
      assertEquals(0, lock.getHoldersCount());
      assertEquals(1, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      lock.requestLock(thread2, LockLevel.WRITE, sink);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(1, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      ThreadUtil.reallySleep(1000);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertEquals(1, lock.getPendingCount());

      lock.removeCurrentHold(thread2);
      lock.nextPending();
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertEquals(0, lock.getPendingCount());

      lock.removeCurrentHold(thread1);
      lock.nextPending();
      assertEquals(0, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertEquals(0, lock.getPendingCount());
    }
  }

  public void testWait() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));
    ThreadID txnId1 = new ThreadID(1);

    ServerThreadContext thread1 = makeTxn(cid1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new TimerSpec(), lockMgr, sink);

    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());
  }

  public void testWaitOnAndNotify() throws Exception {
    ClientID cid1 = new ClientID(new ChannelID(1));

    ServerThreadContext thread1 = makeTxn(cid1, new ThreadID(1));
    ServerThreadContext thread2 = makeTxn(cid1, new ThreadID(2));

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new TimerSpec(), lockMgr, sink);
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    // make sure another thread can notify()
    boolean granted = lock.requestLock(thread2, LockLevel.WRITE, sink);
    assertTrue(granted);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.notify(thread2, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(1, lock.getPendingCount());

    lock.removeCurrentHold(thread2);
    lock.nextPending();
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(0, lock.getPendingCount());

    // make sure the wait()'er gets an write lock back
    Collection holders = lock.getHoldersCollection();
    assertEquals(1, holders.size());
    Holder holder = null;
    for (Iterator iter = holders.iterator(); iter.hasNext();) {
      assertNull(holder); // should only be one holder
      holder = (Holder) iter.next();
    }

    assertEquals(thread1, holder.getThreadContext());
    //assertFalse(holder.isUpgrade());
    assertEquals(LockLevel.WRITE, holder.getLockLevel());
  }

  public void testNotifyAll() throws Exception {
    Lock lock = createLockWithIndefiniteWaits(100);

    assertEquals(0, lock.getHoldersCount());
    assertEquals(100, lock.getWaiterCount());
    assertEquals(0, lock.getPendingCount());

    ServerThreadContext notifier = makeTxn(getUniqueClientID(), getUniqueTransactionID());
    lock.requestLock(notifier, LockLevel.WRITE, sink);
    lock.notify(notifier, true, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(100, lock.getPendingCount());

    lock.removeCurrentHold(notifier);
    lock.nextPending();
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(99, lock.getPendingCount());
  }

  public void testNotify() throws Exception {
    Lock lock = createLockWithIndefiniteWaits(3);

    assertEquals(0, lock.getHoldersCount());
    assertEquals(3, lock.getWaiterCount());
    assertEquals(0, lock.getPendingCount());

    ServerThreadContext notifier = makeTxn(getUniqueClientID(), getUniqueTransactionID());
    lock.requestLock(notifier, LockLevel.WRITE, sink);
    lock.notify(notifier, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(2, lock.getWaiterCount());
    assertEquals(1, lock.getPendingCount());

    lock.notify(notifier, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertEquals(2, lock.getPendingCount());

    lock.notify(notifier, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(3, lock.getPendingCount());

    // one more time, should have no effect
    lock.notify(notifier, false, notifiedWaiters);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(3, lock.getPendingCount());

    lock.removeCurrentHold(notifier);
    lock.nextPending();
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertEquals(2, lock.getPendingCount());
  }

  private ClientID getUniqueClientID() {
    return new ClientID(new ChannelID(uniqueId++));
  }

  private ThreadID getUniqueTransactionID() {
    return new ThreadID(uniqueId++);
  }

  private Lock createLockWithIndefiniteWaits(int numWaits) throws TCIllegalMonitorStateException {
    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});

    for (int i = 0; i < numWaits; i++) {
      int before = lock.getWaiterCount();

      ServerThreadContext me = makeTxn(getUniqueClientID(), getUniqueTransactionID());
      lock.requestLock(me, LockLevel.WRITE, sink);
      lock.wait(me, waitTimer, new TimerSpec(), lockMgr, sink);
      assertEquals(before + 1, lock.getWaiterCount());
    }

    assertEquals(numWaits, lock.getWaiterCount());
    assertEquals(0, lock.getHoldersCount());
    assertEquals(0, lock.getPendingCount());

    return lock;
  }

  public void testAddPending() throws Exception {
    LockEventMonitor monitor = new LockEventMonitor();
    Lock lock = new Lock(new LockID("yo"), 0, new LockEventListener[] { monitor });

    ServerThreadContext thread1 = makeTxn(getUniqueClientID(), getUniqueTransactionID());
    ServerThreadContext thread2 = makeTxn(getUniqueClientID(), getUniqueTransactionID());

    lock.requestLock(thread1, LockLevel.WRITE, sink);
    lock.requestLock(thread2, LockLevel.WRITE, sink);

    // make sure that addPending(...) causes notifyAddPending to be called with
    // the correct pending count and
    // for each holder's lock award context.
    //
    // The pending count should now be 1.
    int pendingCount = 1;

    Collection holders = lock.getHoldersCollection();
    assertEquals(1, holders.size());
    checkNotifyAddPendingCallContextForAllHolders(lock, monitor, pendingCount, holders);
  }

  private void checkNotifyAddPendingCallContextForAllHolders(Lock lock, LockEventMonitor monitor, int waiterCount,
                                                             Collection holders) throws Exception {
    for (Iterator iter = holders.iterator(); iter.hasNext();) {
      Holder holder = (Holder) iter.next();
      checkCallContext(monitor.waitForNotifyAddPending(0), lock.getLockID(), holder.getNodeID(), waiterCount);
    }
  }

  private void checkCallContext(CallContext cc, LockID theLockId, NodeID nodeID, int waiterCount) {
    assertNotNull(cc);
    LockAwardContext ac = cc.ctxt;
    assertEquals(theLockId, ac.getLockID());
    assertEquals(nodeID, ac.getNodeID());
    assertEquals(waiterCount, cc.waiterCount);
  }

}
