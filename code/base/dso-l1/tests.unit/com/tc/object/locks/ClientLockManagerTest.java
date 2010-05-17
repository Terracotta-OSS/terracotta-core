/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.exception.TCRuntimeException;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Info;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.locks.TestRemoteLockManager.LockResponder;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.session.TestSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockState;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.runtime.ThreadIDManagerImpl;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.runtime.ThreadIDMapJdk15;
import com.tc.util.runtime.ThreadIDMapUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ClientLockManagerTest extends TCTestCase {
  private ClientLockManagerImpl  lockManager;
  private TestRemoteLockManager  rmtLockManager;
  private TestSessionManager     sessionManager;
  private ManualThreadIDManager  threadManager;
  
  private final GroupID         gid = new GroupID(0);

  public ClientLockManagerTest() {
    //
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sessionManager = new TestSessionManager();
    rmtLockManager = new TestRemoteLockManager(sessionManager);
    threadManager = new ManualThreadIDManager();
    
    lockManager = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, rmtLockManager, threadManager, new NullClientLockManagerConfig(), ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    rmtLockManager.setClientLockManager(lockManager);
  }

  public void testRunGC() {
    NullClientLockManagerConfig testClientLockManagerConfig = new NullClientLockManagerConfig(100);

    final ClientLockManagerImpl clientLockManagerImpl = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, rmtLockManager, threadManager,                                                                                  
                                                                                  testClientLockManagerConfig, ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    rmtLockManager.setClientLockManager(clientLockManagerImpl);

    final LockID lockID1 = new StringLockID("1");
    final ThreadID threadID1 = new ThreadID(1);

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
        new Thread() {
          @Override
          public void run() {
            clientLockManagerImpl.award(gid, sessionManager.getSessionID(gid), lock, ThreadID.VM_ID, level);
          }
        }.start();
      }
    };

    threadManager.setThreadID(threadID1);
    clientLockManagerImpl.lock(lockID1, LockLevel.WRITE);
    clientLockManagerImpl.unlock(lockID1, LockLevel.WRITE);

    ThreadUtil.reallySleep(400);

    assertEquals(0, clientLockManagerImpl.runLockGc());

    // now change the timeout to a much higher number
    testClientLockManagerConfig.setTimeoutInterval(Long.MAX_VALUE);

    clientLockManagerImpl.lock(lockID1, LockLevel.WRITE);

    clientLockManagerImpl.unlock(lockID1, LockLevel.WRITE);

    assertEquals(1, clientLockManagerImpl.runLockGc());
  }

  public void testRunGCWithAHeldLock() {
    NullClientLockManagerConfig testClientLockManagerConfig = new NullClientLockManagerConfig(100);

    final ClientLockManagerImpl clientLockManagerImpl = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, rmtLockManager,
                                                                                  threadManager, testClientLockManagerConfig, ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    rmtLockManager.setClientLockManager(clientLockManagerImpl);

    final LockID lockID1 = new StringLockID("1");
    final LockID lockID2 = new StringLockID("2");
    final ThreadID threadID1 = new ThreadID(1);

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
        new Thread() {
          @Override
          public void run() {
            clientLockManagerImpl.award(gid, sessionManager.getSessionID(gid), lock, ThreadID.VM_ID, level);
          }
        }.start();
      }
    };

    // Hold lock 1
    threadManager.setThreadID(threadID1);
    clientLockManagerImpl.lock(lockID1, LockLevel.WRITE);

    // Grab and release lock 2
    clientLockManagerImpl.lock(lockID2, LockLevel.WRITE);
    clientLockManagerImpl.unlock(lockID2, LockLevel.WRITE);

    ThreadUtil.reallySleep(400);
    

    // One lock should be GCed
    assertEquals(1, clientLockManagerImpl.runLockGc());

    // now unlock lock 1
    clientLockManagerImpl.unlock(lockID1, LockLevel.WRITE);

    ThreadUtil.reallySleep(400);

    // Both should be GCed
    assertEquals(0, clientLockManagerImpl.runLockGc());
  }

  /**
   * testing accessOrder for LinkedHashMap which ClientHashMap extends
   */
  public void testClientHashMap() {
    LinkedHashMap linkedHashMap = new LinkedHashMap(4, 0.75f, true);

    linkedHashMap.put("key1", "value1");
    linkedHashMap.put("key2", "value2");
    linkedHashMap.put("key3", "value3");
    linkedHashMap.put("key4", "value4");

    // do two reads
    linkedHashMap.get("key1");
    linkedHashMap.get("key2");

    Iterator iter = linkedHashMap.values().iterator();
    assertEquals((String) iter.next(), "value3");
    assertEquals((String) iter.next(), "value4");
    assertEquals((String) iter.next(), "value1");
    assertEquals((String) iter.next(), "value2");

    linkedHashMap = new LinkedHashMap(4, 0.75f, false);
    linkedHashMap.put("key1", "value1");
    linkedHashMap.put("key2", "value2");
    linkedHashMap.put("key3", "value3");
    linkedHashMap.put("key4", "value4");

    // do two reads
    linkedHashMap.get("key1");
    linkedHashMap.get("key2");

    iter = linkedHashMap.values().iterator();
    assertEquals((String) iter.next(), "value1");
    assertEquals((String) iter.next(), "value2");
    assertEquals((String) iter.next(), "value3");
    assertEquals((String) iter.next(), "value4");

  }

  public void testNestedSynchronousWrite() {
    final LockID lockID_1 = new StringLockID("1");
    final LockID lockID_2 = new StringLockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    threadManager.setThreadID(threadID_1);    
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, LockLevel.WRITE);
    lockManager.lock(lockID_1, LockLevel.READ);
    lockManager.lock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    lockManager.lock(lockID_1, LockLevel.WRITE);
    lockManager.lock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.WRITE);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.READ);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.WRITE);

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksGreedy();

    threadManager.setThreadID(threadID_2);    
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_2, LockLevel.WRITE);
    lockManager.lock(lockID_2, LockLevel.READ);
    lockManager.lock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    lockManager.lock(lockID_2, LockLevel.WRITE);
    lockManager.lock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, LockLevel.WRITE);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, LockLevel.READ);
    lockManager.unlock(lockID_2, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getFlushCount());
    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testSynchronousWriteUnlock() {
    final LockID lockID_1 = new StringLockID("1");
    final LockID lockID_2 = new StringLockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    threadManager.setThreadID(threadID_1);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertTrue(rmtLockManager.getFlushCount() > 0);

    rmtLockManager.makeLocksGreedy();

    rmtLockManager.resetFlushCount();

    threadManager.setThreadID(threadID_2);    
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertTrue(rmtLockManager.getFlushCount() > 0);

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testSynchronousWriteWait() {

    final LockID lockID_1 = new StringLockID("1");
    final LockID lockID_2 = new StringLockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    threadManager.setThreadID(threadID_1);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());

    NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    LockWaiter waiterThread = new LockWaiter(barrier,  lockID_1, threadID_1, -1);
    
    waiterThread.start();
    Object o = barrier.take();
    assertNotNull(o);

    assertTrue(rmtLockManager.getFlushCount() > 0);

    rmtLockManager.makeLocksGreedy();
    rmtLockManager.resetFlushCount();

    threadManager.setThreadID(threadID_2);
    lockManager.lock(lockID_2, LockLevel.SYNCHRONOUS_WRITE);
    assertEquals(0, rmtLockManager.getFlushCount());

    waiterThread = new LockWaiter(barrier, lockID_2, threadID_2, -1);
    waiterThread.start();
    o = barrier.take();
    assertNotNull(o);

    assertTrue(rmtLockManager.getFlushCount() > 0);

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testTryLock() {
    class TryLockRemoteLockManager extends TestRemoteLockManager {
      private final CyclicBarrier requestBarrier;
      private final CyclicBarrier awardBarrier;

      public TryLockRemoteLockManager(final SessionProvider sessionProvider, final CyclicBarrier requestBarrier,
                                      final CyclicBarrier awardBarrier) {
        super(sessionProvider);
        this.requestBarrier = requestBarrier;
        this.awardBarrier = awardBarrier;
      }

      @Override
      public void tryLock(final LockID lockID, final ThreadID threadID, final ServerLockLevel level, final long timeout) {
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
      private final CyclicBarrier awardBarrier;

      public TryLockClientLockManager(final TCLogger logger, final SessionManager sessionManager, 
                                      final RemoteLockManager remoteLockManager, final ThreadIDManager threadManager,
                                      final CyclicBarrier awardBarrier) {
        super(logger, sessionManager, remoteLockManager, threadManager, new NullClientLockManagerConfig(), ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
        this.awardBarrier = awardBarrier;
      }

      @Override
      public void award(final NodeID nid, final SessionID sessionID, final LockID lockID, final ThreadID threadID, final ServerLockLevel level) {
        try {
          awardBarrier.barrier();
          super.award(nid, sessionID, lockID, threadID, level);
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

    lockManager = new TryLockClientLockManager(new NullTCLogger(), sessionManager, rmtLockManager, threadManager, awardBarrier);

    final LockID lockID1 = new StringLockID("1");
    final ThreadID txID = new ThreadID(1);

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          requestBarrier.barrier();
          lockManager.award(gid, sessionManager.getSessionID(gid), lockID1, ThreadID.VM_ID, ServerLockLevel.WRITE);
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    });
    t1.start();

    threadManager.setThreadID(txID);
    try {
      lockManager.tryLock(lockID1, LockLevel.WRITE, 0L);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void testGreedyLockRequest() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
        queue.put(new Object[] {lock, thread, level});
        new Thread() {
          @Override
          public void run() {
            lockManager.award(gid, sessionManager.getSessionID(gid), lock, ThreadID.VM_ID, level);
          }
        }.start();
      }
    };

    threadManager.setThreadID(tx1);
    lockManager.lock(lockID1, LockLevel.WRITE); // Goes to RemoteLockManager

    Object[] request = (Object[]) queue.poll(1000l);
    assertNotNull(request);
    assertEquals(lockID1, request[0]);
    assertEquals(tx1, request[1]);
    assertEquals(ServerLockLevel.WRITE, request[2]);

    // None of these should end up in RemoteLockManager
    lockManager.lock(lockID1, LockLevel.READ);
    lockManager.unlock(lockID1, LockLevel.READ);
    lockManager.unlock(lockID1, LockLevel.WRITE);
    lockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx2);
    lockManager.lock(lockID1, LockLevel.READ);

    assertNull(queue.poll(1000l));
  }

  public void testNotified() throws Exception {
    final LockID lockID1 = new StringLockID("3");
    final LockID lockID2 = new StringLockID("4");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    lockManager.lock(lockID1, LockLevel.WRITE);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();

    // In order to wait on a lock, we must first request and be granted the
    // write lock. The TestRemoteLockManager
    // takes care of awarding the lock when we ask for it.
    //
    // We don't add this lock request to the set of held locks because the
    // call to wait moves it to being not
    // held anymore.
    System.err.println("LockManager lock" + System.identityHashCode(lockManager));
    threadManager.setThreadID(tx2);
    lockManager.lock(lockID2, LockLevel.WRITE);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    final LockWaiter waiterThread = new LockWaiter(barrier, lockID2, tx2, 0);
    waiterThread.start();

    barrier.take();
    assertTrue(barrier.isEmpty());
    if (!waiterThread.exceptions.isEmpty()) {
      for (Iterator i = waiterThread.exceptions.iterator(); i.hasNext();) {
        ((Throwable) i.next()).printStackTrace();
      }
      fail("Waiter thread had exceptions!");
    }

    for (ClientServerExchangeLockContext cselc : lockManager.getAllLockContexts()) {
      switch (cselc.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          Assert.assertEquals(lockID1, cselc.getLockID());
          Assert.assertEquals(cselc.getThreadID(), tx1);
          Assert.assertEquals(cselc.getState().getLockLevel(), ServerLockLevel.WRITE);
          break;
        case WAITER:
          Assert.assertEquals(lockID2, cselc.getLockID());
          Assert.assertEquals(tx2, cselc.getThreadID());
          Assert.assertEquals(0, cselc.timeout());
          break;
        case PENDING_READ:
        case PENDING_WRITE:
          Assert.fail();
          break;
        default:
          break;
          
      }
    }

    // Make sure there are no pending lock requests
    rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());

    // now call notified() and make sure that the appropriate waits become
    // pending requests
    lockManager.notified(lockID2, tx2);

    Thread.sleep(1000);
    
    for (ClientServerExchangeLockContext cselc : lockManager.getAllLockContexts()) {
      switch (cselc.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          Assert.assertEquals(lockID1, cselc.getLockID());
          Assert.assertEquals(tx1, cselc.getThreadID());
          Assert.assertEquals(ServerLockLevel.WRITE, cselc.getState().getLockLevel());
          break;
        case WAITER:
          Assert.fail();
          break;
        case PENDING_READ:
        case PENDING_WRITE:
          Assert.assertEquals(lockID2, cselc.getLockID());
          Assert.assertEquals(tx2, cselc.getThreadID());
          Assert.assertEquals(ServerLockLevel.WRITE, cselc.getState().getLockLevel());
          break;
        default:
          break;          
      }
    }

    // now make sure that if you award the lock, the right stuff happens
    lockManager.award(gid, sessionManager.getSessionID(gid), lockID2, tx2, ServerLockLevel.WRITE);

    waiterThread.join();
    
    for (ClientServerExchangeLockContext cselc : lockManager.getAllLockContexts()) {
      switch (cselc.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          LockID lid = cselc.getLockID();
          if (lockID1.equals(lid)) {
            Assert.assertEquals(lockID1, cselc.getLockID());
            Assert.assertEquals(tx1, cselc.getThreadID());
            Assert.assertEquals(ServerLockLevel.WRITE, cselc.getState().getLockLevel());
          } else if (lockID2.equals(lid)) {
            Assert.assertEquals(lockID2, cselc.getLockID());
            Assert.assertEquals(tx2, cselc.getThreadID());
            Assert.assertEquals(ServerLockLevel.WRITE, cselc.getState().getLockLevel());
          } else {
            Assert.fail();
          }
          break;
        case WAITER:
          Assert.fail();
          break;
        case PENDING_READ:
        case PENDING_WRITE:
          Assert.fail();
          break;
        default:
          break;          
      }
    }
    
    // the lock should have been awarded and no longer pending

    // this doesn't make sense against the new impl
    //assertTrue(rmtLockManager.lockRequestCalls.isEmpty());
  }

  public void testAddAllOutstandingLocksTo() throws Exception {

    // XXX: The current TestRemoteLockManager doesn't handle multiple
    // read-locks by different transactions properly,
    // so this test doesn't test that case.
    final LockID lockID = new StringLockID("my lock");
    final ThreadID tx1 = new ThreadID(1);
    final LockLevel writeLockLevel = LockLevel.WRITE;

    final LockID readLock = new StringLockID("my read lock");
    final ThreadID tx2 = new ThreadID(2);
    final LockLevel readLockLevel = LockLevel.READ;

    // final LockID synchWriteLock = new LockID("my synch write lock");
    // final ThreadID tx3 = new ThreadID(3);
    // final int synchWriteLockLevel = LockLevel.SYNCHRONOUS_WRITE;

    threadManager.setThreadID(tx1);
    lockManager.lock(lockID, writeLockLevel);
    threadManager.setThreadID(tx2);
    lockManager.lock(readLock, readLockLevel);
    // lockManager.lock(synchWriteLock, tx3, synchWriteLockLevel);

    for (ClientServerExchangeLockContext cselc : lockManager.getAllLockContexts()) {
      switch (cselc.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          LockID lid = cselc.getLockID();
          if (lockID.equals(lid)) {
            Assert.assertEquals(lockID, cselc.getLockID());
            Assert.assertEquals(tx1, cselc.getThreadID());
            Assert.assertEquals(ServerLockLevel.WRITE, cselc.getState().getLockLevel());
          } else if (readLock.equals(lid)) {
            Assert.assertEquals(readLock, cselc.getLockID());
            Assert.assertEquals(tx2, cselc.getThreadID());
            Assert.assertEquals(ServerLockLevel.READ, cselc.getState().getLockLevel());
          } else {
            Assert.fail();
          }
          break;
        default:
          break;          
      }
    }

    threadManager.setThreadID(tx1);
    lockManager.unlock(lockID, writeLockLevel);
    threadManager.setThreadID(tx2);
    lockManager.unlock(readLock, readLockLevel);
    // lockManager.unlock(synchWriteLock, tx3);
    for (ClientServerExchangeLockContext cselc : lockManager.getAllLockContexts()) {
      switch (cselc.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          Assert.fail();
          break;
        default:
          break;          
      }
    }
  }

//  public void testAddAllOutstandingWaitersTo() throws Exception {
//
//    final LockInfoDumpHandler lockInfoDumpHandler = new LockInfoDumpHandler() {
//
//      public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
//        lockManager.addAllLocksTo(lockInfo);
//      }
//
//      public ThreadIDMap getThreadIDMap() {
//        return threadManager.getThreadIDMap();
//      }
//    };
//
//    final L1Info l1info = new L1Info(lockInfoDumpHandler);
//
//    final LockID lockID = new StringLockID("my lock");
//    final ThreadID tx1 = new ThreadID(1);
//    final TimerSpec waitInvocation = new TimerSpec();
//    final Object waitObject = new Object();
//    final NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
//    lockManager.lock(lockID, tx1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
//    Thread t = new LockWaiter(barrier, lockID, threadLockManager, waitInvocation, waitObject);
//    t.start();
//    barrier.take();
//    ThreadUtil.reallySleep(200);
//
//    Set s = new HashSet();
//    lockManager.addAllWaitersTo(s, GroupID.NULL_ID);
//    List waiters = new LinkedList(s);
//    String threadDump = l1info.takeThreadDump(System.currentTimeMillis());
//    assertEquals(1, waiters.size());
//    WaitLockRequest waitRequest = (WaitLockRequest) waiters.get(0);
//    assertEquals(lockID, waitRequest.lockID());
//    assertEquals(tx1, waitRequest.threadID());
//    assertEquals(waitInvocation, waitRequest.getTimerSpec());
//
//    if (threadDump.indexOf("require JRE-1.5 or greater") < 0) {
//      Assert.eval("The text \"WAITING ON LOCK: [LockID(my lock)]\" should be present in the thread dump", threadDump
//          .indexOf("WAITING ON LOCK: [LockID(my lock)]") >= 0);
//    }
//
//    // The lock this waiter was in when wait was called should no longer be
//    // outstanding.
//    assertEquals(0, lockManager.addAllHeldLocksTo(new HashSet(), GroupID.NULL_ID).size());
//  }

  public void testPauseBlocks() throws Exception {
    final LinkedQueue flowControl = new LinkedQueue();
    final LinkedQueue lockComplete = new LinkedQueue();
    final LinkedQueue unlockComplete = new LinkedQueue();
    final LockID lockID = new StringLockID("1");
    final ThreadID txID = new ThreadID(1);
    final LockLevel lockType = LockLevel.WRITE;

    final List lockerException = new ArrayList();
    Thread locker = new Thread("LOCKER") {
      @Override
      public void run() {
        try {
          threadManager.setThreadID(txID);
          flowControl.put("locker: Calling lock");
          lockManager.lock(lockID, lockType);
          lockComplete.put("locker: lock complete.");

          // wait until I'm allowed to unlock...
          System.out.println(flowControl.take());
          lockManager.unlock(lockID, lockType);
          unlockComplete.put("locker: unlock complete.");

          // wait until I'm allowed to call lock() again
          System.out.println(flowControl.take());
          rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
          threadManager.setThreadID(txID);
          lockManager.lock(lockID, lockType);
          System.out.println("locker: Done calling lock again");

        } catch (Throwable e) {
          e.printStackTrace();
          lockerException.add(e);
        }
      }
    };

    pause();
    locker.start();

    // wait until the locker has a chance to start up...
    System.out.println(flowControl.take());

    ThreadUtil.reallySleep(500);

    // make sure it hasn't returned from the lock call.
    assertTrue(lockComplete.peek() == null);

    unpause();

    // make sure the call to lock(..) completes
    System.out.println(lockComplete.take());
    System.out.println("Done testing lock(..)");

    // now pause again and allow the locker to call unlock...
    pause();
    flowControl.put("test: lock manager paused, it's ok for locker to call unlock(..)");
    ThreadUtil.reallySleep(500);
    assertTrue(unlockComplete.peek() == null);

    // now UN-pause and make sure the locker returns from unlock(..)
    unpause();

    unlockComplete.take();
    System.out.println("Done testing unlock(..)");

    // TODO: test awardLock() and the other public methods I didn't have
    // time to test...

    // assert locker thread never threw an exception
    assertTrue("Locker thread threw an exception: " + lockerException, lockerException.isEmpty());
  }

  public void testResendBasics() throws Exception {
    final List requests = new ArrayList();
    final LinkedQueue flowControl = new LinkedQueue();
    final SynchronizedBoolean respond = new SynchronizedBoolean(true);
    final List lockerException = new ArrayList();

    rmtLockManager.lockResponder = new LockResponder() {
      public void respondToLockRequest(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
        new Thread() {
          @Override
          public void run() {
            requests.add(new Object[] {lock, thread, level});
            if (respond.get()) {
              lockManager.award(gid, sessionManager.getSessionID(gid), lock, thread, level);
            }
            try {
              flowControl.put("responder: respondToLockRequest complete.  Lock awarded: " + respond.get());
            } catch (InterruptedException e) {
              e.printStackTrace();
              lockerException.add(e);
            }
          }
        }.start();
      }
    };

    final ThreadID tid0 = new ThreadID(0);
    final ThreadID tid1 = new ThreadID(1);
    final LockID lid0 = new StringLockID("0");
    final LockID lid1 = new StringLockID("1");

    // request a lock that gets a response
    Thread t = new LockGetter(lid0, tid0, LockLevel.WRITE);
    t.start();
    // wait until the lock responder finishes...
    System.out.println(flowControl.take());
    t.join();
    assertEquals(1, requests.size());
    Object[] request = (Object[]) requests.get(0);
    assertNotNull(request);
    assertEquals(lid0, request[0]);
    assertEquals(tid0, request[1]);
    assertEquals(ServerLockLevel.WRITE, request[2]);

    // now request a lock that doesn't get a response.
    requests.clear();
    respond.set(false);

    t = new LockGetter(lid1, tid1, LockLevel.WRITE);
    t.start();
    System.out.println(flowControl.take());

    assertEquals(1, requests.size());
    request = (Object[]) requests.get(0);
    assertNotNull(request);
    assertEquals(lid1, request[0]);
    assertEquals(tid1, request[1]);
    assertEquals(ServerLockLevel.WRITE, request[2]);

    // resend outstanding lock requests and respond to them.
    requests.clear();
    respond.set(true);

    Collection<ClientServerExchangeLockContext> contexts = lockManager.getAllLockContexts();
    int pendingCount = 0;
    for (ClientServerExchangeLockContext c : contexts) {
      if (c.getState().getType() == Type.PENDING) {
        assertEquals(c.getState(), ServerLockContext.State.PENDING_WRITE);
        assertEquals(c.getLockID(), lid1);
        assertEquals(c.getThreadID(), tid1);
        pendingCount++;
      }
    }
    assertEquals(1, pendingCount);

    // there should be no outstanding lock requests.
    // calling requestOutstanding() should cause no lock requests.

    requests.clear();
    rmtLockManager.lockResponder = rmtLockManager.LOOPBACK_LOCK_RESPONDER;

    // assert locker thread never threw an exception
    assertTrue("Locker thread threw an exception: " + lockerException, lockerException.isEmpty());
  }

  public void testAwardWhenNotPending() throws Exception {
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);
    lockManager.award(gid, sessionManager.getSessionID(gid), lockID, txID, ServerLockLevel.WRITE);
  }

  public void testBasics() throws Exception {
    final ThreadID tid0 = new ThreadID(0);
    final LockID lid0 = new StringLockID("0");

    final ThreadID tid1 = new ThreadID(1);

    System.out.println("Get lock0 for tx0");
    threadManager.setThreadID(tid0);
    lockManager.lock(lid0, LockLevel.WRITE);
    System.out.println("Got lock0 for tx0");
    lockManager.lock(lid0, LockLevel.WRITE);
    System.out.println("Got lock0 for tx0 AGAIN so the recursion lock is correct");
    final boolean[] done = new boolean[1];
    done[0] = false;
    Thread t = new Thread() {
      @Override
      public void run() {
        threadManager.setThreadID(tid1);
        lockManager.lock(lid0, LockLevel.WRITE);
        System.out.println("Got lock0 for tx1");
        done[0] = true;
      }
    };
    t.start();
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    threadManager.setThreadID(tid0);
    lockManager.unlock(lid0, LockLevel.WRITE);
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    lockManager.unlock(lid0, LockLevel.WRITE);
    ThreadUtil.reallySleep(500);
    assertTrue(done[0]);
  }

  public void testAllLockInfoInThreadDump() throws Exception {

    final ThreadIDMap threadIDMap = ThreadIDMapUtil.getInstance();
    final SessionManager session = new TestSessionManager();
    final TestRemoteLockManager remote = new TestRemoteLockManager(sessionManager);
    final ClientLockManager clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), session, remote, new ThreadIDManagerImpl(threadIDMap), new NullClientLockManagerConfig(), ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    remote.setClientLockManager(clientLockManager);
    
    final LockInfoDumpHandler lockInfoDumpHandler = new LockInfoDumpHandler() {

      public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
        for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
          switch (c.getState().getType()) {
            case GREEDY_HOLDER:
            case HOLDER:
              lockInfo.addLock(LockState.HOLDING, c.getThreadID(), c.getLockID().toString());
              break;
            case WAITER:
              lockInfo.addLock(LockState.WAITING_ON, c.getThreadID(), c.getLockID().toString());
              break;
            case TRY_PENDING:
            case PENDING:
              lockInfo.addLock(LockState.WAITING_TO, c.getThreadID(), c.getLockID().toString());
              break;
          }
        }
      }

      public ThreadIDMap getThreadIDMap() {
        return threadIDMap;
      }

    };

    final L1Info l1info = new L1Info(lockInfoDumpHandler);
    final LockID lid0 = new StringLockID("Locky0");
    final LockID lid1 = new StringLockID("Locky1");
    final LockID lid2 = new StringLockID("Locky2");
    final LockID lid3 = new StringLockID("Locky3");

    final CyclicBarrier txnBarrier = new CyclicBarrier(3);

    final Latch[] done = new Latch[3];
    for (int i = 0; i < done.length; i++) {
      done[i] = new Latch();
    }

    Thread.currentThread().setName("terracotta_thread");
    clientLockManager.lock(lid0, LockLevel.WRITE);
    System.out.println("XXX TERRA Thread : Got WRITE lock0 for tx0");

    clientLockManager.lock(lid0, LockLevel.WRITE);
    System.out.println("XXX TERRA Thread : Again .. Got WRITE lock0 for tx0");

    clientLockManager.lock(lid1, LockLevel.READ);
    System.out.println("XXX TERRA Thread : Got READ lock1 for tx0");

    Thread t1 = new Thread("yahoo_thread") {
      @Override
      public void run() {
        clientLockManager.lock(lid3, LockLevel.WRITE);
        System.out.println("XXX YAHOO Thread : Got WRITE lock3 for tx1");

        clientLockManager.lock(lid0, LockLevel.WRITE);
        System.out.println("XXX YAHOO Thread : Got WRITE lock0 for tx1");

        try {
          txnBarrier.barrier();
        } catch (Exception e) {
          throw new AssertionError(e);
        }

        /*
         * threadLockManager.unlock(lid0); System.out.println("XXX YAHOO Thread : Released WRITE lock0 for tx1");
         */

        clientLockManager.unlock(lid3, LockLevel.WRITE);
        System.out.println("XXX YAHOO Thread : Released WRITE lock3 for tx1");

        done[1].release();
      }
    };

    Thread t2 = new Thread("google_thread") {
      @Override
      public void run() {
        clientLockManager.lock(lid2, LockLevel.WRITE);
        System.out.println("XXX GOOGL Thread : Got WRITE lock2 for tx2");

        try {
          txnBarrier.barrier();
        } catch (Exception e) {
          throw new AssertionError(e);
        }

        clientLockManager.lock(lid1, LockLevel.WRITE);
        System.out.println("XXX GOOGL Thread : Got WRITE lock1 for tx2");
        done[2].release();
      }
    };

    t1.start();
    t2.start();

    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    // pauseAndStart();
    String threadDump = l1info.takeThreadDump(System.currentTimeMillis());

    if (threadDump.indexOf("require JRE-1.5 or greater") < 0) {
      Assert.eval("The text \"LOCKED : [StringLockID(Locky2)]\" should be present in the thread dump", threadDump
          .indexOf("LOCKED : [StringLockID(Locky2)]") >= 0);

      Assert.eval("The text \"LOCKED : [StringLockID(Locky3)]\" should be present in the thread dump", threadDump
          .indexOf("LOCKED : [StringLockID(Locky3)]") >= 0);

      Assert.eval("The text \"WAITING TO LOCK: [StringLockID(Locky0)]\" should be present in the thread dump", threadDump
          .indexOf("WAITING TO LOCK: [StringLockID(Locky0)]") >= 0);

      Assert.eval(threadDump.indexOf("LOCKED : [StringLockID(Locky0), StringLockID(Locky0), StringLockID(Locky1)]") >= 0);
    }

    clientLockManager.unlock(lid0, LockLevel.WRITE);
    System.out.println("XXX TERRA Thread : Released WRITE lock0 for tx0");
    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    clientLockManager.unlock(lid0, LockLevel.WRITE);
    System.out.println("XXX TERRA  Thread : Again Released WRITE lock0 for tx0");
    clientLockManager.unlock(lid1, LockLevel.READ);
    System.out.println("XXX TERRA Thread : Released READ lock1 for tx0");
    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    txnBarrier.barrier();

    done[1].acquire();
    done[2].acquire();
  }
  
  /**
   * CDV-1262: overriding Thread.getId() is a bad idea, but people do it anyway;
   * our locks should still function correctly although dump information may not 
   * be available.
   */
  public void testOverriddenThreadId() throws Exception {
    
    final ThreadIDMap threadIDMap = ThreadIDMapUtil.getInstance();
    final SessionManager session = new TestSessionManager();
    final TestRemoteLockManager remote = new TestRemoteLockManager(sessionManager);
    final ClientLockManager clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), session, remote, new ThreadIDManagerImpl(threadIDMap), new NullClientLockManagerConfig(), ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    remote.setClientLockManager(clientLockManager);
    
    final LockID lid0 = new StringLockID("Locky0");
    final boolean[] success = new boolean[2];
    
    Thread t0 = new Thread("peaches_thread") {
      @Override
      public long getId() {
        return 0xCAFED00DL;
      }
      
      @Override
      public void run() {
        clientLockManager.lock(lid0, LockLevel.WRITE);
        clientLockManager.unlock(lid0, LockLevel.WRITE);
        success[0] = true;
      }
    };
    
    t0.start();
    t0.join();
    assertTrue(success[0]);
    
    Thread t1 = new Thread("herb_thread") {
      @Override
      public long getId() {
        return 0xCAFED00DL;
      }
      
      @Override
      public void run() {
        clientLockManager.lock(lid0, LockLevel.WRITE);
        clientLockManager.unlock(lid0, LockLevel.WRITE);
        success[1] = true;
      }
    };
    
    t1.start();
    t1.join();
    assertTrue(success[1]);
  }
  
  /**
   * CDV-1262: dead threads should not remain in the ThreadIDMap.
   */
  public void testThreadIDMapCleanup() throws Exception {
    final ThreadIDMapJdk15 threadIDMap = (ThreadIDMapJdk15)ThreadIDMapUtil.getInstance();
    final int numThreads = 10000;
    int originalSize = threadIDMap.getSize();
    int size = makeLotsOfThreads(numThreads);
    assertTrue(size == originalSize + numThreads);
    ThreadUtil.reallySleep(200);
    System.gc();
    assertTrue(threadIDMap.getSize() < size);
  }
  
  /**
   * Start and stop a bunch of threads.
   * Keep all the thread references around till the routine is exited.
   * @return the size of the ThreadIDMap when all the threads are still ref'd.
   */
  private int makeLotsOfThreads(final int numThreads) throws Exception {
    final ThreadIDMapJdk15 threadIDMap = (ThreadIDMapJdk15)ThreadIDMapUtil.getInstance();
    final SessionManager session = new TestSessionManager();
    final TestRemoteLockManager remote = new TestRemoteLockManager(sessionManager);
    final ClientLockManager clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), session, remote, new ThreadIDManagerImpl(threadIDMap), new NullClientLockManagerConfig(), ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
    remote.setClientLockManager(clientLockManager);
    
    final LockID lid0 = new StringLockID("Locky0");
    Thread threads[] = new Thread[numThreads];
    for (int i = 0; i < numThreads; ++i) {
      threads[i] = new Thread() {
        @Override
        public void run() {
          clientLockManager.lock(lid0, LockLevel.WRITE);
          clientLockManager.unlock(lid0, LockLevel.WRITE);
        }
      };
      threads[i].start();
      threads[i].join();
    }
    return threadIDMap.getSize();
  }
  
  public void testBasicUnlock() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new StringLockID("0");

    threadManager.setThreadID(tid0);
    lockManager.lock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.lock(lid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, LockLevel.WRITE);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(2, rmtLockManager.getUnlockRequestCount());
  }

  public void testLockUpgradeMakesRemoteRequest() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new StringLockID("0");

    threadManager.setThreadID(tid0);
    lockManager.lock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    // upgrade lock
    try {
      lockManager.lock(lid0, LockLevel.WRITE);
      throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      // expected
    }
    assertEquals(1, rmtLockManager.getLockRequestCount());
  }

  public void testNestedReadLocksGrantsLocally() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new StringLockID("0");

    threadManager.setThreadID(tid0);
    lockManager.lock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    final int count = 25;

    for (int i = 0; i < count; i++) {
      // get nested read locks
      lockManager.lock(lid0, LockLevel.READ);
      assertEquals(1, rmtLockManager.getLockRequestCount());
    }

    for (int i = 0; i < count; i++) {
      lockManager.unlock(lid0, LockLevel.READ);
      assertEquals(1, rmtLockManager.getLockRequestCount());
      assertEquals(0, rmtLockManager.getUnlockRequestCount());
    }

    lockManager.unlock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  public void testUnlockAfterDowngrade() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new StringLockID("0");

    threadManager.setThreadID(tid0);
    lockManager.lock(lid0, LockLevel.WRITE);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    // downgrade lock
    lockManager.lock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, LockLevel.READ);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, LockLevel.WRITE);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  private void pause() {
    lockManager.pause(GroupID.ALL_GROUPS, 1);
  }

  private void unpause() {
    lockManager.initializeHandshake(ClientID.NULL_ID, GroupID.ALL_GROUPS, new TestClientHandshakeMessage());
    lockManager.unpause(GroupID.ALL_GROUPS, 0);
  }

  public static void main(final String[] args) {
    //
  }

  public class LockWaiter extends Thread implements WaitListener {
    private final LockID                 lid;
    private final ThreadID               tid;
    private final long                   timeout;
    private final NoExceptionLinkedQueue preWaitSignalQueue;
    private final List                   exceptions = new LinkedList();
    private final ClientLockManagerImpl  clientLockManager;

    private LockWaiter(final NoExceptionLinkedQueue preWaitSignalQueue, final LockID lid, final ThreadID threadID, final long timeout) {
      this(preWaitSignalQueue, lid, null, threadID, timeout);
    }

    private LockWaiter(final NoExceptionLinkedQueue preWaitSignalQueue, final LockID lid,
                       final ClientLockManagerImpl clientLockManager, final ThreadID threadID, final long timeout) {
      this.preWaitSignalQueue = preWaitSignalQueue;
      this.lid = lid;
      this.tid = threadID;
      this.clientLockManager = clientLockManager;
      this.timeout = timeout;
      this.setName("LockWaiter");
    }

    @Override
    public void run() {
      try {
        threadManager.setThreadID(tid);

        if (clientLockManager != null) {
          if (timeout < 0) {
            clientLockManager.wait(lid, this, null);
          } else {
            clientLockManager.wait(lid, this, null, timeout);
          }
        } else {
          if (timeout == 0) {
            lockManager.wait(lid, this, null);
          } else {
            lockManager.wait(lid, this, null, timeout);
          }
        }
      } catch (Throwable t) {
        t.printStackTrace();
        exceptions.add(t);
      }

      ThreadUtil.reallySleep(2000);
    }

    public void handleWaitEvent() {
      preWaitSignalQueue.put(new Object());
    }

    public Collection getExceptions() {
      return this.exceptions;
    }
  }
  
  private class LockGetter extends Thread {
    LockID    lid;
    ThreadID  tid;
    LockLevel lockType;

    private LockGetter(final LockID lid, final ThreadID tid, final LockLevel lockType) {
      this.lid = lid;
      this.tid = tid;
      this.lockType = lockType;
    }

    @Override
    public void run() {
      threadManager.setThreadID(tid);
      lockManager.lock(lid, lockType);
    }
  }
}
