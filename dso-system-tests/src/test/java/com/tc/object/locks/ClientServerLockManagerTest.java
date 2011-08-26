/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.logging.NullTCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.session.TestSessionManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.NullChannelManager;
import com.tc.objectserver.locks.ServerLockContextBean;
import com.tc.objectserver.locks.factory.NonGreedyLockPolicyFactory;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

public class ClientServerLockManagerTest extends TestCase {

  private ClientLockManagerImpl       clientLockManager;
  private LockManagerImpl             serverLockManager;
  private ClientServerLockManagerGlue glue;
  private TestSessionManager          sessionManager;
  private ManualThreadIDManager       threadManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sessionManager = new TestSessionManager();
    TestSink sink = new TestSink();
    glue = new ClientServerLockManagerGlue(sessionManager, sink, new NonGreedyLockPolicyFactory());
    threadManager = new ManualThreadIDManager();
    clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, glue, threadManager,
                                                  new NullClientLockManagerConfig(),
                                                  ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);

    serverLockManager = new LockManagerImpl(sink, new NullChannelManager(), new NonGreedyLockPolicyFactory());
    glue.set(clientLockManager, serverLockManager);
  }

  public void testRWServer() {
    final LockID lockID1 = new StringLockID("1");
    final LockID lockID2 = new StringLockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final ThreadID tx3 = new ThreadID(3);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx3);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID2, LockLevel.READ);
    // clientLockManager.lock(lockID2, tx2, LockLevel.WRITE); // Upgrade
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWRServer() {
    final LockID lockID1 = new StringLockID("1");
    final LockID lockID2 = new StringLockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final ThreadID tx3 = new ThreadID(3);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx3);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID2, LockLevel.WRITE);
    clientLockManager.lock(lockID2, LockLevel.READ);

    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testLockWaitWriteServer() throws Exception {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    final CyclicBarrier barrier = new CyclicBarrier(2);
    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, new WaitListener() {
            public void handleWaitEvent() {
              try {
                barrier.barrier();
              } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(e);
              }
            }
          }, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    barrier.barrier();
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitWRServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);
    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyRWServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    clientLockManager.unlock(lockID1, LockLevel.WRITE);
    sleep(1000l);
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyRWClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();

    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    clientLockManager.unlock(lockID1, LockLevel.WRITE);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx1)) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Should not have READ lock level."); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }
    LockMBean[] lockBeans2 = serverLockManager.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyWRClientServer() throws Exception {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();

    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          System.out.println("1st thread going to wait on lock id = " + lockID1 + " thread = " + tx1);
          clientLockManager.wait(lockID1, null);
          System.out.println("1st thread Wait over for lock id = " + lockID1 + " thread = " + tx1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();

    threadManager.setThreadID(tx2);
    System.out.println("2nd thread trying to acquire lock = " + lockID1 + " thread = " + tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */

    System.out.println("2nd thread got the lock and trying to notify = " + lockID1 + " thread = " + tx2);

    glue.notify(lockID1, tx2, true);

    System.out.println("2nd thread unlocking = " + lockID1 + " thread = " + tx2);

    clientLockManager.unlock(lockID1, LockLevel.WRITE);

    System.out.println("2nd thread unlocked = " + lockID1 + " thread = " + tx2);

    ThreadUtil.reallySleep(3000);
    dumpClientState();
    LockMBean[] lockBeans2 = serverLockManager.getAllLocks();
    for (LockMBean lockBean : lockBeans2) {
      System.out.println("Lock on the server " + lockBean);
    }

    waitCallThread.join();

    threadManager.setThreadID(tx1);

    Assert.assertTrue(clientLockManager.isLockedByCurrentThread(lockID1, LockLevel.WRITE));
    Assert.assertTrue(clientLockManager.isLockedByCurrentThread(lockID1, LockLevel.READ));
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  private void dumpClientState() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    PrettyPrinterImpl prettyPrinter = new PrettyPrinterImpl(pw);
    prettyPrinter.autoflush(false);
    prettyPrinter.visit(clientLockManager);
    writer.flush();

    StringBuffer buffer = writer.getBuffer();
    System.out.println(buffer);
    
    System.out.println("Thread dump ------- ");
    System.out.println(ThreadDumpUtil.getThreadDump());
  }

  public void testPendingWaitNotifiedRWClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx2)) {
        // if (LockLevel.isRead(request.lockLevel()) || !LockLevel.isWrite(request.lockLevel())) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Server Lock Level is not WRITE only on tx2 the client side"); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }

    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testPendingWaitNotifiedWRClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    Thread waitCallThread = new Thread() {

      @Override
      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, null);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx2)) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Server Lock Level is not WRITE only on tx2 the client side"); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testPendingRequestClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    Thread pendingLockRequestThread = new Thread() {

      @Override
      public void run() {
        threadManager.setThreadID(tx1);
        clientLockManager.lock(lockID1, LockLevel.WRITE);
      }
    };
    pendingLockRequestThread.start();
    sleep(1000l);
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWRClient() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ); // Local
    // Upgrade
    clientLockManager.unlock(lockID1, LockLevel.READ); // should release READ

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx1)) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Lock Level is not WRITE only"); }
        found = true;
        break;
      }
    }
    if (!found) { throw new AssertionError("Didn't find the lock I am looking for"); }
  }

  public void testConcurrentLocksServerRestart() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.CONCURRENT);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.CONCURRENT);
    LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    LockManagerImpl server2 = glue.restartServer();
    LockMBean[] lockBeans2 = server2.getAllLocks();
    if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  private boolean equals(LockMBean[] lockBeans1, LockMBean[] lockBeans2) {
    if (lockBeans1.length != lockBeans2.length) { return false; }
    for (int i = 0; i < lockBeans1.length; i++) {
      LockID lockName1 = lockBeans1[i].getLockID();
      boolean found = false;
      for (int j = 0; j < lockBeans2.length; j++) {
        LockID lockName2 = lockBeans2[j].getLockID();
        if (lockName1.equals(lockName2)) {
          if (!equals(lockBeans1[i], lockBeans2[j])) { return false; }
          found = true;
          break;
        }
      }
      if (!found) { return false; }
    }
    return true;
  }

  private boolean equals(LockMBean bean1, LockMBean bean2) {
    return equals(bean1.getContexts(), bean2.getContexts()) && bean1.getLockID().equals(bean2.getLockID());
  }

  private boolean equals(ServerLockContextBean[] contexts1, ServerLockContextBean[] contexts2) {
    if (contexts1.length != contexts2.length) { return false; }
    for (ServerLockContextBean context1 : contexts1) {
      boolean found = false;
      for (ServerLockContextBean context2 : contexts2) {
        if (context2.equals(context1)) {
          found = true;
          break;
        }
      }
      if (!found) return false;
    }
    return true;
  }

  private void sleep(long l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      // NOP
    }
  }

  private void handleExceptionForTest(Exception e) {
    e.printStackTrace();
    throw new AssertionError(e);
  }

  @Override
  protected void tearDown() throws Exception {
    glue.stop();
    super.tearDown();
  }
}
