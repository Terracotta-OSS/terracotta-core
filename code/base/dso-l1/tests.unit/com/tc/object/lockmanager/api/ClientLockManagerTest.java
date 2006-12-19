/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.object.lockmanager.api.TestRemoteLockManager.LockResponder;
import com.tc.object.lockmanager.impl.ClientLockManagerImpl;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.session.TestSessionManager;
import com.tc.object.tx.WaitInvocation;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class ClientLockManagerTest extends TestCase {
  private ClientLockManager     lockManager;

  private TestRemoteLockManager rmtLockManager;

  private TestSessionManager    sessionManager;

  protected void setUp() throws Exception {
    super.setUp();
    sessionManager = new TestSessionManager();
    rmtLockManager = new TestRemoteLockManager(sessionManager);
    lockManager = new ClientLockManagerImpl(new NullTCLogger(), rmtLockManager, sessionManager);
    rmtLockManager.setClientLockManager(lockManager);
  }

  public void testTryLock() {
    class TryLockRemoteLockManager extends TestRemoteLockManager {
      private CyclicBarrier requestBarrier;
      private CyclicBarrier awardBarrier;

      public TryLockRemoteLockManager(SessionProvider sessionProvider, CyclicBarrier requestBarrier, CyclicBarrier awardBarrier) {
        super(sessionProvider);
        this.requestBarrier = requestBarrier;
        this.awardBarrier = awardBarrier;
      }

      public void tryRequestLock(LockID lockID, ThreadID threadID, int lockType) {
        try {
          requestBarrier.barrier();
          awardBarrier.barrier();
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    class TryLockClientLockManager extends ClientLockManagerImpl {
      private CyclicBarrier awardBarrier;

      public TryLockClientLockManager(TCLogger logger, RemoteLockManager remoteLockManager,
                                      SessionManager sessionManager, CyclicBarrier awardBarrier) {
        super(logger, remoteLockManager, sessionManager);
        this.awardBarrier = awardBarrier;
      }

      public void awardLock(SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
        try {
          awardBarrier.barrier();
          super.awardLock(sessionID, lockID, threadID, level);
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    final CyclicBarrier requestBarrier = new CyclicBarrier(2);
    final CyclicBarrier awardBarrier = new CyclicBarrier(2);

    rmtLockManager = new TryLockRemoteLockManager(sessionManager, requestBarrier, awardBarrier);
    lockManager = new TryLockClientLockManager(new NullTCLogger(), rmtLockManager, sessionManager, awardBarrier);

    final LockID lockID1 = new LockID("1");
    final ThreadID txID = new ThreadID(1);

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          requestBarrier.barrier();
          lockManager.awardLock(sessionManager.getSessionID(), lockID1, ThreadID.VM_ID, LockLevel
              .makeGreedy(LockLevel.WRITE));
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    });
    t1.start();

    lockManager.tryLock(lockID1, txID, LockLevel.WRITE);

  }

  public void testGreedyLockRequest() {
    final LockID lockID1 = new LockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(LockRequest request) {
        queue.put(request);
        lockManager.awardLock(sessionManager.getSessionID(), request.lockID(), ThreadID.VM_ID, LockLevel
            .makeGreedy(request.lockLevel()));
      }
    };

    lockManager.lock(lockID1, tx1, LockLevel.WRITE); // Goes to RemoteLockManager

    LockRequest request = (LockRequest) queue.poll(1000l);
    assertNotNull(request);
    assertEquals(tx1, request.threadID());
    assertEquals(lockID1, request.lockID());
    assertEquals(LockLevel.WRITE, request.lockLevel());

    // None of these should end up in RemoteLockManager
    lockManager.lock(lockID1, tx1, LockLevel.READ);
    lockManager.unlock(lockID1, tx1);
    lockManager.unlock(lockID1, tx1);
    lockManager.lock(lockID1, tx1, LockLevel.READ);
    lockManager.lock(lockID1, tx2, LockLevel.READ);

    assertNull(queue.poll(1000l));
  }

  public void testNotified() throws Exception {
    final LockID lockID1 = new LockID("1");
    final LockID lockID2 = new LockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final Set heldLocks = new HashSet();
    final Set waiters = new HashSet();

    heldLocks.add(new LockRequest(lockID1, tx1, LockLevel.WRITE));
    lockManager.lock(lockID1, tx1, LockLevel.WRITE);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    WaitInvocation waitInvocation = new WaitInvocation();

    // In order to wait on a lock, we must first request and be granted the
    // write lock. The TestRemoteLockManager
    // takes care of awarding the lock when we ask for it.
    //
    // We don't add this lock request to the set of held locks because the
    // call to wait moves it to being not
    // held anymore.
    lockManager.lock(lockID2, tx2, LockLevel.WRITE);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    WaitLockRequest waitLockRequest = new WaitLockRequest(lockID2, tx2, LockLevel.WRITE, waitInvocation);
    waiters.add(waitLockRequest);
    final LockWaiter waiterThread = new LockWaiter(barrier, waitLockRequest, new Object());
    waiterThread.start();

    barrier.take();
    assertTrue(barrier.isEmpty());
    if (!waiterThread.exceptions.isEmpty()) {
      for (Iterator i = waiterThread.exceptions.iterator(); i.hasNext();) {
        ((Throwable) i.next()).printStackTrace();
      }
      fail("Waiter thread had exceptions!");
    }

    // pause the lock manager in preparation for pulling interrogating the
    // state...
    pauseAndStart();

    Set s = new HashSet();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(waiters, s);
    s.clear();

    lockManager.addAllPendingLockRequestsTo(s);
    assertTrue(s.size() == 0);

    // Make sure there are no pending lock requests
    rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());

    lockManager.unpause();

    // now call notified() and make sure that the appropriate waits become
    // pending requests
    lockManager.notified(waitLockRequest.lockID(), waitLockRequest.threadID());

    pauseAndStart();
    // The held locks should be the same
    s.clear();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    // the lock waits should be empty
    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(Collections.EMPTY_SET, s);

    lockManager.addAllPendingLockRequestsTo(s);
    assertTrue(s.size() == 1);
    LockRequest lr = (LockRequest) s.iterator().next();
    assertNotNull(lr);
    assertEquals(waitLockRequest.lockID(), lr.lockID());
    assertEquals(waitLockRequest.threadID(), lr.threadID());
    assertTrue(waitLockRequest.lockLevel() == lr.lockLevel());

    lockManager.unpause();

    // now make sure that if you award the lock, the right stuff happens
    lockManager.awardLock(sessionManager.getSessionID(), waitLockRequest.lockID(), waitLockRequest.threadID(),
                          waitLockRequest.lockLevel());
    heldLocks.add(waitLockRequest);

    pauseAndStart();
    // the held locks should contain the newly awarded, previously notified
    // lock.
    s.clear();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    // there should still be no waiters
    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(Collections.EMPTY_SET, s);

    // the lock should have been awarded and no longer pending
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());
    lockManager.addAllPendingLockRequestsTo(null);
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());
  }

  public void testAddAllOutstandingLocksTo() throws Exception {

    // XXX: The current TestRemoteLockManager doesn't handle multiple
    // read-locks by different transactions properly,
    // so this test doesn't test that case.
    final LockID lockID = new LockID("my lock");
    final ThreadID tx1 = new ThreadID(1);
    final int writeLockLevel = LockLevel.WRITE;

    final LockID readLock = new LockID("my read lock");
    final ThreadID tx2 = new ThreadID(2);
    final int readLockLevel = LockLevel.READ;

    Set lockRequests = new HashSet();
    lockRequests.add(new LockRequest(lockID, tx1, writeLockLevel));
    lockRequests.add(new LockRequest(readLock, tx2, readLockLevel));

    lockManager.lock(lockID, tx1, writeLockLevel);
    lockManager.lock(readLock, tx2, readLockLevel);

    Set s = new HashSet();
    try {
      lockManager.addAllHeldLocksTo(s);
      fail("Expected an assertion error.");
    } catch (AssertionError e) {
      // expected
    }

    pauseAndStart();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(lockRequests.size(), s.size());
    assertEquals(lockRequests, s);

    lockManager.unpause();
    lockManager.unlock(lockID, tx1);
    lockManager.unlock(readLock, tx2);
    pauseAndStart();
    assertEquals(0, lockManager.addAllHeldLocksTo(new HashSet()).size());
  }

  public void testAddAllOutstandingWaitersTo() throws Exception {
    final LockID lockID = new LockID("my lock");
    final ThreadID tx1 = new ThreadID(1);
    final WaitInvocation waitInvocation = new WaitInvocation();
    final Object waitObject = new Object();
    final NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    lockManager.lock(lockID, tx1, LockLevel.WRITE);
    Thread t = new LockWaiter(barrier, lockID, tx1, waitInvocation, waitObject);
    t.start();
    barrier.take();
    ThreadUtil.reallySleep(200);

    Set s = new HashSet();
    try {
      lockManager.addAllWaitersTo(s);
      fail("Expected an assertion error.");
    } catch (AssertionError e) {
      // expected
    }

    pauseAndStart();
    lockManager.addAllWaitersTo(s);
    List waiters = new LinkedList(s);
    assertEquals(1, waiters.size());
    WaitLockRequest waitRequest = (WaitLockRequest) waiters.get(0);
    assertEquals(lockID, waitRequest.lockID());
    assertEquals(tx1, waitRequest.threadID());
    assertEquals(waitInvocation, waitRequest.getWaitInvocation());

    // The lock this waiter was in when wait was called should no longer be
    // outstanding.
    assertEquals(0, lockManager.addAllHeldLocksTo(new HashSet()).size());
  }

  public void testPauseBlocks() throws Exception {
    final LinkedQueue flowControl = new LinkedQueue();
    final LinkedQueue lockComplete = new LinkedQueue();
    final LinkedQueue unlockComplete = new LinkedQueue();
    final LockID lockID = new LockID("1");
    final ThreadID txID = new ThreadID(1);
    final int lockType = LockLevel.WRITE;

    Thread locker = new Thread("LOCKER") {
      public void run() {
        try {
          flowControl.put("locker: Calling lock");
          lockManager.lock(lockID, txID, lockType);
          lockComplete.put("locker: lock complete.");

          // wait until I'm allowed to unlock...
          System.out.println(flowControl.take());
          lockManager.unlock(lockID, txID);
          unlockComplete.put("locker: unlock complete.");

          // wait until I'm allowed to call lock() again
          System.out.println(flowControl.take());
          rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
          lockManager.lock(lockID, txID, lockType);
          System.out.println("locker: Done calling lock again");

        } catch (Throwable e) {
          e.printStackTrace();
          fail();
        }
      }
    };

    assertFalse(lockManager.isStarting());
    pauseAndStart();
    locker.start();

    // wait until the locker has a chance to start up...
    System.out.println(flowControl.take());

    ThreadUtil.reallySleep(500);

    // make sure it hasn't returned from the lock call.
    assertTrue(lockComplete.peek() == null);
    // unpause...
    assertTrue(lockManager.isStarting());
    lockManager.unpause();
    assertFalse(lockManager.isStarting());
    // make sure the call to lock(..) completes
    System.out.println(lockComplete.take());
    System.out.println("Done testing lock(..)");

    // now pause again and allow the locker to call unlock...
    pauseAndStart();
    flowControl.put("test: lock manager paused, it's ok for locker to call unlock(..)");
    ThreadUtil.reallySleep(500);
    assertTrue(unlockComplete.peek() == null);
    // now unpause and make sure the locker returns from unlock(..)
    lockManager.unpause();
    unlockComplete.take();
    System.out.println("Done testing unlock(..)");

    // TODO: test awardLock() and the other public methods I didn't have
    // time to
    // test...
  }

  public void testResendBasics() throws Exception {
    final List requests = new ArrayList();
    final LinkedQueue flowControl = new LinkedQueue();
    final SynchronizedBoolean respond = new SynchronizedBoolean(true);
    rmtLockManager.lockResponder = new LockResponder() {
      public void respondToLockRequest(final LockRequest request) {
        new Thread() {
          public void run() {
            requests.add(request);
            if (respond.get()) {
              lockManager.awardLock(sessionManager.getSessionID(), request.lockID(), request.threadID(), request
                  .lockLevel());
            }
            try {
              flowControl.put("responder: respondToLockRequest complete.  Lock awarded: " + respond.get());
            } catch (InterruptedException e) {
              e.printStackTrace();
              fail();
            }
          }
        }.start();
      }
    };

    final ThreadID tid0 = new ThreadID(0);
    final ThreadID tid1 = new ThreadID(1);
    final LockID lid0 = new LockID("0");
    final LockID lid1 = new LockID("1");

    LockRequest lr0 = new LockRequest(lid0, tid0, LockLevel.WRITE);
    LockRequest lr1 = new LockRequest(lid1, tid1, LockLevel.WRITE);

    // request a lock that gets a response
    Thread t = new LockGetter(lid0, tid0, LockLevel.WRITE);
    t.start();
    // wait until the lock responder finishes...
    System.out.println(flowControl.take());
    assertEquals(1, requests.size());
    assertEquals(lr0, requests.get(0));

    // now request a lock that doesn't get a response.
    requests.clear();
    respond.set(false);

    t = new LockGetter(lid1, tid1, LockLevel.WRITE);
    t.start();
    System.out.println(flowControl.take());

    assertEquals(1, requests.size());
    assertEquals(lr1, requests.get(0));

    // resend outstanding lock requests and respond to them.
    requests.clear();
    respond.set(true);
    pauseAndStart();
    lockManager.addAllPendingLockRequestsTo(requests);
    lockManager.unpause();
    assertEquals(1, requests.size());
    assertEquals(lr1, requests.get(0));

    // there should be no outstanding lock requests.
    // calling requestOutstanding() should cause no lock requests.

    requests.clear();
    rmtLockManager.lockResponder = rmtLockManager.LOOPBACK_LOCK_RESPONDER;

  }

  public void testAwardWhenNotPending() throws Exception {
    LockID lockID = new LockID("1");
    ThreadID txID = new ThreadID(1);
    try {
      lockManager.awardLock(sessionManager.getSessionID(), lockID, txID, LockLevel.WRITE);
      fail("Should have thrown an error");
    } catch (AssertionError e) {
      // expected
    }
  }

  public void testBasics() throws Exception {
    final ThreadID tid0 = new ThreadID(0);
    final LockID lid0 = new LockID("0");

    final ThreadID tid1 = new ThreadID(1);

    System.out.println("Get lock0 for tx0");
    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    System.out.println("Got lock0 for tx0");
    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    System.out.println("Got lock0 for tx0 AGAIN so the recursion lock is correct");
    final boolean[] done = new boolean[1];
    done[0] = false;
    Thread t = new Thread() {
      public void run() {
        lockManager.lock(lid0, tid1, LockLevel.WRITE);
        System.out.println("Got lock0 for tx1");
        done[0] = true;
      }
    };
    t.start();
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    lockManager.unlock(lid0, tid0);
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    lockManager.unlock(lid0, tid0);
    ThreadUtil.reallySleep(500);
    assertTrue(done[0]);
  }

  public void testBasicUnlock() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(2, rmtLockManager.getUnlockRequestCount());
  }

  public void testLockChangesAfterUpgrade() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    // upgrade lock
    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());

    // get more locks (should see no requests to L2)
    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(2, rmtLockManager.getLockRequestCount());
  }

  public void testLockUpgradeMakesRemoteRequest() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    // upgrade lock
    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());
  }

  public void testNestedReadLocksGrantsLocally() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    final int count = 25;

    for (int i = 0; i < count; i++) {
      // get nested read locks
      lockManager.lock(lid0, tid0, LockLevel.READ);
      assertEquals(1, rmtLockManager.getLockRequestCount());
    }

    for (int i = 0; i < count; i++) {
      lockManager.unlock(lid0, tid0);
      assertEquals(1, rmtLockManager.getLockRequestCount());
      assertEquals(0, rmtLockManager.getUnlockRequestCount());
    }

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  public void testUnlockAfterDowngrade() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.WRITE);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    // downgrade lock
    lockManager.lock(lid0, tid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  private void pauseAndStart() {
    lockManager.pause();
    lockManager.starting();
  }

  public static void main(String[] args) {
    //
  }

  public class LockWaiter extends Thread implements WaitListener {
    private final LockID                 lid;

    private final ThreadID               tid;

    private final WaitInvocation         call;

    private final NoExceptionLinkedQueue preWaitSignalQueue;

    private final Object                 waitObject;

    private final List                   exceptions = new LinkedList();

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, WaitLockRequest request, Object waitObject) {
      this(preWaitSignalQueue, request.lockID(), request.threadID(), request.getWaitInvocation(), waitObject);
    }

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, LockID lid, ThreadID threadID, WaitInvocation call,
                       Object waitObject) {
      this.preWaitSignalQueue = preWaitSignalQueue;
      this.lid = lid;
      this.tid = threadID;
      this.waitObject = waitObject;
      this.call = call;
    }

    public void run() {
      try {
        lockManager.wait(lid, tid, call, waitObject, this);
      } catch (Throwable t) {
        exceptions.add(t);
      }
    }

    public void handleWaitEvent() {
      preWaitSignalQueue.put(new Object());
    }

    public Collection getExceptions() {
      return this.exceptions;
    }
  }

  private class LockGetter extends Thread {
    LockID   lid;
    ThreadID tid;
    int      lockType;

    private LockGetter(LockID lid, ThreadID tid, int lockType) {
      this.lid = lid;
      this.tid = tid;
      this.lockType = lockType;
    }

    public void run() {
      lockManager.lock(lid, tid, lockType);
    }

  }
}
