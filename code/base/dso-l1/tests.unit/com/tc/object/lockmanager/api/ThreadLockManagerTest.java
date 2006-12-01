/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.lockmanager.impl.ThreadLockManagerImpl;
import com.tc.object.tx.WaitInvocation;

import junit.framework.TestCase;

public class ThreadLockManagerTest extends TestCase {

  private TestLockManager   lm;
  private ThreadLockManager tlm;

  public void setUp() throws Exception {
    lm = new TestLockManager();
    tlm = new ThreadLockManagerImpl(lm);
  }

  public void testLockIDFor() {
    String lockName = "fo0";
    tlm.lockIDFor(lockName);
    assertEquals(lockName, lm.lockIDForCalls.get(0));
  }

  public void testLock() throws Exception {
    final LockID lockID = new LockID("lock");
    final int lockLevel = LockLevel.WRITE;
    assertEquals(0, lm.locks.size());
    tlm.lock(lockID, lockLevel);
    Object[] args = getLockCallArgs();

    verifyLockArgs(lockID, new ThreadID(1), lockLevel, args);

    tlm.lock(lockID, lockLevel);
    args = getLockCallArgs();
    // calling lock in the same thread should result in the same thread id being used to call lock with.
    verifyLockArgs(lockID, new ThreadID(1), lockLevel, args);

    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable getter = new Runnable() {
      public void run() {
        tlm.lock(lockID, lockLevel);
        try {
          barrier.barrier();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };

    new Thread(getter).start();
    barrier.barrier();

    args = getLockCallArgs();
    verifyLockArgs(lockID, new ThreadID(2), lockLevel, args);

    new Thread(getter).start();
    barrier.barrier();

    args = getLockCallArgs();
    verifyLockArgs(lockID, new ThreadID(3), lockLevel, args);
  }

  private Object[] getLockCallArgs() {
    assertEquals(1, lm.locks.size());
    Object[] args = (Object[]) lm.locks.get(0);
    lm.locks.clear();
    return args;
  }

  private void verifyLockArgs(LockID lockID, ThreadID threadID, int lockLevel, Object[] args) {
    assertEquals(lockID, args[0]);
    assertEquals(threadID, args[1]);
    assertEquals(new Integer(lockLevel), args[2]);
  }

  public void testWait() throws Exception {
    final LockID lockID = new LockID("lock");
    final WaitInvocation call = new WaitInvocation();
    final Object lockObject = new Object();
    final WaitListener waitListener = null;
    tlm.wait(lockID, call, lockObject, waitListener);

    verifyWaitArgs(lockID, new ThreadID(1), call, lockObject, waitListener, getWaitCallArgs());

    // try it again in the same thread. The thread id should remain the same.
    tlm.wait(lockID, call, lockObject, waitListener);
    verifyWaitArgs(lockID, new ThreadID(1), call, lockObject, waitListener, getWaitCallArgs());

    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable waiter = new Runnable() {
      public void run() {
        tlm.wait(lockID, call, lockObject, waitListener);
        try {
          barrier.barrier();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };

    // try it in a different thread; the thread id should increment.
    new Thread(waiter).start();
    barrier.barrier();
    verifyWaitArgs(lockID, new ThreadID(2), call, lockObject, waitListener, getWaitCallArgs());

    new Thread(waiter).start();
    barrier.barrier();
    verifyWaitArgs(lockID, new ThreadID(3), call, lockObject, waitListener, getWaitCallArgs());

  }

  private void verifyWaitArgs(LockID lockID, ThreadID threadID, WaitInvocation call, Object lockObject,
                              WaitListener waitListener, Object[] args) {
    assertEquals(lockID, args[0]);
    assertEquals(threadID, args[1]);
    assertEquals(call, args[2]);
    assertEquals(lockObject, args[3]);
    assertEquals(waitListener, args[4]);
  }

  private Object[] getWaitCallArgs() {
    assertEquals(1, lm.waitCalls.size());
    Object[] args = (Object[]) lm.waitCalls.get(0);
    lm.waitCalls.clear();
    return args;
  }

  public void testNotify() throws Exception {
    final boolean notifyAll = false;
    final LockID lockID = new LockID("lock");

    tlm.notify(lockID, notifyAll);
    verifyNotifyArgs(lockID, new ThreadID(1), notifyAll, getNotifyCallArgs());
    // the same thread should have the same thread id.
    tlm.notify(lockID, notifyAll);
    verifyNotifyArgs(lockID, new ThreadID(1), notifyAll, getNotifyCallArgs());

    final CyclicBarrier barrier = new CyclicBarrier(2);
    Runnable notifier = new Runnable() {

      public void run() {
        tlm.notify(lockID, notifyAll);
        try {
          barrier.barrier();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

    };

    new Thread(notifier).start();
    barrier.barrier();
    verifyNotifyArgs(lockID, new ThreadID(2), notifyAll, getNotifyCallArgs());

    // different threads should have different thread ids.
    new Thread(notifier).start();
    barrier.barrier();
    verifyNotifyArgs(lockID, new ThreadID(3), notifyAll, getNotifyCallArgs());

  }

  private void verifyNotifyArgs(LockID lockID, ThreadID threadID, boolean notifyAll, Object[] args) {
    assertEquals(lockID, args[0]);
    assertEquals(threadID, args[1]);
    assertEquals(new Boolean(notifyAll), args[2]);
  }

  private Object[] getNotifyCallArgs() {
    assertEquals(1, lm.notifyCalls.size());
    Object[] args = (Object[]) lm.notifyCalls.get(0);
    lm.notifyCalls.clear();
    return args;
  }

  public void testUnlock() throws Exception {
    final LockID lockID = new LockID("lock");
    tlm.unlock(lockID);
    verifyUnlockArgs(lockID, new ThreadID(1), getUnlockArgs());

    tlm.unlock(lockID);
    verifyUnlockArgs(lockID, new ThreadID(1), getUnlockArgs());

    final CyclicBarrier barrier = new CyclicBarrier(2);
    Runnable unlocker = new Runnable() {
      public void run() {
        try {
          tlm.unlock(lockID);
          barrier.barrier();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };

    new Thread(unlocker).start();
    barrier.barrier();
    verifyUnlockArgs(lockID, new ThreadID(2), getUnlockArgs());

    new Thread(unlocker).start();
    barrier.barrier();
    verifyUnlockArgs(lockID, new ThreadID(3), getUnlockArgs());
  }

  private void verifyUnlockArgs(LockID lockID, ThreadID threadID, Object[] args) {
    assertEquals(lockID, args[0]);
    assertEquals(threadID, args[1]);
  }

  private Object[] getUnlockArgs() {
    assertEquals(1, lm.unlockCalls.size());
    Object[] args = (Object[]) lm.unlockCalls.get(0);
    lm.unlockCalls.clear();
    return args;
  }

}
