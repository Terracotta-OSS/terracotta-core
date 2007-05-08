/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;


import com.tc.async.api.Sink;
import com.tc.async.impl.MockSink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.lockmanager.impl.WaitTimerImpl;
import com.tc.object.tx.WaitInvocation;
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
  private WaitTimer       waitTimer;
  private LockManagerImpl lockMgr  = new LockManagerImpl(new NullChannelManager());
  private NotifiedWaiters notifiedWaiters;

  protected void setUp() throws Exception {
    super.setUp();
    this.notifiedWaiters = new NotifiedWaiters();
    this.sink = new MockSink();
    this.waitTimer = new WaitTimerImpl();
  }

  public void testLockClear() {
    // XXX: test the return value of lock.nextPending()
    // XXX: add this check into the other tests too

    // throw new ImplementMe();
  }

  public void testUpgrade() throws Exception {
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);
    ThreadID txnId3 = new ThreadID(3);

    ServerThreadContext thread1 = makeTxn(channelId1, txnId1);
    ServerThreadContext thread2 = makeTxn(channelId1, txnId2);
    ServerThreadContext thread3 = makeTxn(channelId1, txnId3);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.READ, sink);
    lock.requestLock(thread2, LockLevel.READ, sink);
    lock.requestLock(thread3, LockLevel.READ, sink);

    // request the upgrade
    assertEquals(3, lock.getHoldersCount());
    assertEquals(0, lock.getPendingUpgradeCount());
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getPendingUpgradeCount());
    assertEquals(3, lock.getHoldersCount());

    // release 1 of 2 pending read locks
    lock.removeCurrentHold(thread2);
    lock.nextPending();
    assertEquals(1, lock.getPendingUpgradeCount());
    assertEquals(2, lock.getHoldersCount());

    // release final pending read lock
    lock.removeCurrentHold(thread3);
    lock.nextPending();

    // verify upgrade granted
    assertEquals(0, lock.getPendingUpgradeCount());
    assertEquals(1, lock.getHoldersCount());
    Holder holder = (Holder) lock.getHoldersCollection().toArray()[0];
    assertEquals(channelId1, holder.getChannelID());
    assertEquals(txnId1, holder.getThreadID());
    assertTrue(holder.isUpgrade());

    // add some other pending lock requests
    lock.requestLock(thread2, LockLevel.READ, sink);
    lock.requestLock(thread3, LockLevel.WRITE, sink);
    assertEquals(2, lock.getPendingCount());

    // release the upgrade (tx1 and tx2 should now hold read locks)
    lock.removeCurrentHold(thread1);
    lock.awardAllReads();
    assertEquals(1, lock.getPendingCount());
    Holder[] holders = (Holder[]) lock.getHoldersCollection().toArray(new Holder[] {});
    assertEquals(2, holders.length);
    boolean tx1 = false, tx2 = false;

    for (int i = 0; i < 2; i++) {
      Holder h = holders[i];
      assertEquals(channelId1, h.getChannelID());
      if (h.getThreadID().equals(txnId1)) {
        assertFalse(tx1);
        tx1 = true;
      } else if (h.getThreadID().equals(txnId2)) {
        assertFalse(tx2);
        tx2 = true;
      } else {
        fail(h.getThreadID().toString());
      }

      assertEquals(LockLevel.READ, h.getLockLevel());
    }
    assertTrue(tx1);
    assertTrue(tx2);

    // release one of the current read locks
    lock.removeCurrentHold(thread1);
    lock.nextPending();
    assertEquals(1, lock.getHoldersCount());
    assertEquals(1, lock.getPendingCount());

    // release the other read lock
    lock.removeCurrentHold(thread2);
    lock.nextPending();
    assertEquals(0, lock.getPendingCount());
    assertEquals(1, lock.getHoldersCount());
    holder = (Holder) lock.getHoldersCollection().toArray()[0];
    assertEquals(channelId1, holder.getChannelID());
    assertEquals(txnId3, holder.getThreadID());
    assertEquals(LockLevel.WRITE, holder.getLockLevel());
  }

  private static ServerThreadContext makeTxn(ChannelID cid, ThreadID threadID) {
    return new ServerThreadContext(cid, threadID);
  }

  public void testMonitorStateAssertions() throws Exception {
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);

    ServerThreadContext thread1 = makeTxn(channelId1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    lock.wait(thread1, waitTimer, new WaitInvocation(), lockMgr, sink); // indefinite
    // wait()
    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());

    try {
      lock.wait(thread1, waitTimer, new WaitInvocation(), lockMgr, sink);
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
    ChannelID goodChannelID = new ChannelID(1);
    ThreadID goodTxnId = new ThreadID(1);

    ChannelID badChannelID = new ChannelID(2);
    ThreadID badTxnId = new ThreadID(2);

    ServerThreadContext good = makeTxn(goodChannelID, goodTxnId);
    ServerThreadContext bad = makeTxn(badChannelID, badTxnId);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(good, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertFalse(lock.hasPending());

    try {
      // different lock owner should not be to do a wait()
      lock.wait(bad, waitTimer, new WaitInvocation(), lockMgr, sink);
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
      lock.wait(good, waitTimer, new WaitInvocation(), lockMgr, sink);
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
      lock.wait(good, waitTimer, new WaitInvocation(), lockMgr, sink);
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
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);

    ServerThreadContext thread1 = makeTxn(channelId1, txnId1);
    ServerThreadContext thread2 = makeTxn(channelId1, txnId2);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);

    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new WaitInvocation(200), lockMgr, sink);
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
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);
    lockMgr.start();

    ServerThreadContext thread1 = makeTxn(channelId1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    long t1 = System.currentTimeMillis();
    lock.wait(thread1, waitTimer, new WaitInvocation(2000, 500), lockMgr, sink);
    // This is still not perfect - This can only raise false negatives
    // assertEquals(0, lock.getHoldersCount());
    // assertEquals(1, lock.getWaiterCount());
    // assertFalse(lock.hasPending());

    while(lock.getHoldersCount() != 1) {
      ThreadUtil.reallySleep(100);
    }
    long t2 = System.currentTimeMillis();
    assertTrue(t2-t1 >= 2000);
    
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());
  }

  public void testTimedWaitsDontFireWhenLockManagerIsStopped() throws Exception {
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);
    lockMgr.start();

    // Test that a wait() timeout will obtain an uncontended lock
    ServerThreadContext thread1 = makeTxn(channelId1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new WaitInvocation(1000), lockMgr, sink);
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
    ChannelID channelId1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);
    ThreadID txnId2 = new ThreadID(2);
    lockMgr.start();
    {
      // Test that a wait() timeout will obtain an uncontended lock
      ServerThreadContext thread1 = makeTxn(channelId1, txnId1);

      Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
      lock.requestLock(thread1, LockLevel.WRITE, sink);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      long t1 = System.currentTimeMillis();
      lock.wait(thread1, waitTimer, new WaitInvocation(1000), lockMgr, sink);
      // This is still not perfect - This can only raise false negatives
      // assertEquals(0, lock.getHoldersCount());
      // assertEquals(1, lock.getWaiterCount());
      // assertFalse(lock.hasPending());

      while(lock.getHoldersCount() != 1) {
        ThreadUtil.reallySleep(100);
      }
      long t2 = System.currentTimeMillis();
      assertTrue(t2-t1 >= 1000);
      
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());
      
    }

    {
      // this time the wait timeout will cause the waiter to be put in the
      // pending
      // list (instead of instantly getting the lock)
      ServerThreadContext thread1 = makeTxn(channelId1, txnId1);
      ServerThreadContext thread2 = makeTxn(channelId1, txnId2);
      Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
      lock.requestLock(thread1, LockLevel.WRITE, sink);
      assertEquals(1, lock.getHoldersCount());
      assertEquals(0, lock.getWaiterCount());
      assertFalse(lock.hasPending());

      lock.wait(thread1, waitTimer, new WaitInvocation(500), lockMgr, sink);
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
    ChannelID channelID1 = new ChannelID(1);
    ThreadID txnId1 = new ThreadID(1);

    ServerThreadContext thread1 = makeTxn(channelID1, txnId1);

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new WaitInvocation(), lockMgr, sink);

    assertEquals(0, lock.getHoldersCount());
    assertEquals(1, lock.getWaiterCount());
    assertFalse(lock.hasPending());
  }

  public void testWaitOnUpgradedLock() throws Exception {
    ChannelID channelID1 = new ChannelID(1);

    ServerThreadContext thread1 = makeTxn(channelID1, new ThreadID(1));
    ServerThreadContext thread2 = makeTxn(channelID1, new ThreadID(2));

    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});
    lock.requestLock(thread1, LockLevel.READ, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.requestLock(thread1, LockLevel.WRITE, sink);
    assertEquals(1, lock.getHoldersCount());
    assertEquals(0, lock.getWaiterCount());
    assertFalse(lock.hasPending());

    lock.wait(thread1, waitTimer, new WaitInvocation(), lockMgr, sink);
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

    // make sure the wait()'er gets an upgraded lock back
    Collection holders = lock.getHoldersCollection();
    assertEquals(1, holders.size());
    Holder holder = null;
    for (Iterator iter = holders.iterator(); iter.hasNext();) {
      assertNull(holder); // should only be one holder
      holder = (Holder) iter.next();
    }

    assertEquals(thread1, holder.getThreadContext());
    assertTrue(holder.isUpgrade());
    assertEquals(LockLevel.READ | LockLevel.WRITE, holder.getLockLevel());
  }

  public void testNotifyAll() throws Exception {
    Lock lock = createLockWithIndefiniteWaits(100);

    assertEquals(0, lock.getHoldersCount());
    assertEquals(100, lock.getWaiterCount());
    assertEquals(0, lock.getPendingCount());

    ServerThreadContext notifier = makeTxn(getUniqueChannelID(), getUniqueTransactionID());
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

    ServerThreadContext notifier = makeTxn(getUniqueChannelID(), getUniqueTransactionID());
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

  private ChannelID getUniqueChannelID() {
    return new ChannelID(uniqueId++);
  }

  private ThreadID getUniqueTransactionID() {
    return new ThreadID(uniqueId++);
  }

  private Lock createLockWithIndefiniteWaits(int numWaits) throws TCIllegalMonitorStateException {
    Lock lock = new Lock(new LockID("timmy"), 0, new LockEventListener[] {});

    for (int i = 0; i < numWaits; i++) {
      int before = lock.getWaiterCount();

      ServerThreadContext me = makeTxn(getUniqueChannelID(), getUniqueTransactionID());
      lock.requestLock(me, LockLevel.WRITE, sink);
      lock.wait(me, waitTimer, new WaitInvocation(), lockMgr, sink);
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

    ServerThreadContext thread1 = makeTxn(getUniqueChannelID(), getUniqueTransactionID());
    ServerThreadContext thread2 = makeTxn(getUniqueChannelID(), getUniqueTransactionID());

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
      checkCallContext(monitor.waitForNotifyAddPending(0), lock.getLockID(), holder.getChannelID(), waiterCount);
    }
  }

  private void checkCallContext(CallContext cc, LockID theLockId, ChannelID theChannelId, int waiterCount) {
    assertNotNull(cc);
    LockAwardContext ac = cc.ctxt;
    assertEquals(theLockId, ac.getLockID());
    assertEquals(theChannelId, ac.getChannelID());
    assertEquals(waiterCount, cc.waiterCount);
  }

}