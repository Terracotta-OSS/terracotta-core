/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.unit;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.control.ServerControl;
import com.tc.test.TCTestCase;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.restart.AppStateSnapshot;

import java.io.File;
import java.util.Iterator;

public class DSOServerRestartTester extends TCTestCase {

  private RestartTestEnvironment env;
  private long                   startTimeout = 30 * 1000;

  public void setUp() throws Exception {
    super.setUp();
    env = new RestartTestEnvironment(this.getTempDirectory(), new PortChooser(), RestartTestEnvironment.PROD_MODE);
    env.setUp();
  }

  public void tearDown() throws Exception {
    assertNoTestThreadExceptions();
    env.shutdownServer();
  }

  public void testLockState() throws Exception {
    RestartUnitTestApp[] apps = new RestartUnitTestApp[10];
    for (int i = 0; i < apps.length; i++) {
      apps[i] = new DSOServerRestartTestApp(env.getThreadGroup());
    }

    basicTestLockState(apps);

    waitForTestThreads();
  }

  public void testDSOLockState() throws Exception {
    env.newExtraProcessServer();
    env.startServer(startTimeout);
    RestartUnitTestApp[] apps = newTestApps(10);

    basicTestLockState(apps);

    waitForTestThreads();
  }

  private void basicTestLockState(RestartUnitTestApp[] apps) throws Exception {
    Object lck = new Object();
    AppStateSnapshot snapshot = new AppStateSnapshot(apps);
    CyclicBarrier startBarrier = new CyclicBarrier(apps.length + 1);
    for (int i = 0; i < apps.length; i++) {
      final RestartUnitTestApp app = apps[i];
      app.setID(i);
      app.reset();
      app.setSharedLock(lck);
      app.setStartBarrier(startBarrier);
      env.startNewClientThread(new Runnable() {
        public void run() {
          app.attemptLock();
        }
      });
    }

    // wait until all the threads have started.
    startBarrier.barrier();

    while (true) {
      stopServerAndReleaseLock(snapshot);
      System.out.println(snapshot);
      if (!snapshot.allEnded() && !env.hasThreadGroupErrors()) env.startServer(startTimeout);
      else break;
    }
  }

  private void stopServerAndReleaseLock(AppStateSnapshot snapshot) throws Exception {
    RestartUnitTestApp holder = (RestartUnitTestApp) snapshot.getHolder();
    // there was no holder. We're done.
    if (holder == null) {
      if (snapshot.allEnded()) return;
      else fail("No holder, but not all ended: " + snapshot);
    }

    // start the shutdown sequence.
    if (env.getServer().isRunning()) env.getServer().attemptForceShutdown();
    // XXX: Yuck.
    Thread.sleep(2000);
    // release the lock, so the server can shut down.
    holder.fallThrough();
    // wait until the server has stopped...
    while (env.getServer().isRunning()) {
      Thread.sleep(100);
    }
  }

  public void testDSOWaitNotify() throws Exception {
    env.newExtraProcessServer();
    env.startServer(startTimeout);
    RestartUnitTestApp[] apps = newTestApps(10);
    basicTestWaitNotify(apps);
    waitForTestThreads();
  }

  private void basicTestWaitNotify(RestartUnitTestApp[] apps) throws Exception {
    Object lck = new Object();
    AppStateSnapshot snapshot = new AppStateSnapshot(apps);
    CyclicBarrier startBarrier = new CyclicBarrier(1);
    apps[0].setDistributedSharedLock(lck);
    ServerControl server = env.getServer();
    for (int i = 0; i < apps.length; i++) {
      final RestartUnitTestApp app = apps[i];
      app.setID(i);
      app.reset();
      app.setStartBarrier(startBarrier);
      env.startNewClientThread(new Runnable() {
        public void run() {
          app.doWait(0);
        }
      });
    }
    while (!snapshot.allWaiters()) {
      // wait until all the threads have had a chance to wait on the distributed
      // object...
      snapshot.takeSnapshot();
    }

    for (int i = 0; i < apps.length; i++) {
      // call notify() to allow a single app to continue.
      apps[0].doNotify();
      server.shutdown();
      snapshot.takeSnapshot();
      System.out.println(snapshot);
      assertEquals(apps.length - i - 1, snapshot.getWaiters().length);
      if (i + 1 < apps.length) {
        server.start();
      }
    }
    snapshot.takeSnapshot();
    assertTrue(snapshot.allEnded());
  }

  public void testDSOWaitNotifyAll() throws Exception {
    env.newExtraProcessServer().start();
    RestartUnitTestApp[] apps = newTestApps(10);
    basicTestWaitNotifyAll(apps);
    waitForTestThreads();
  }

  public void testDSOTimedWait() throws Exception {
    env.newExtraProcessServer().start();
    RestartUnitTestApp[] apps = newTestApps(10);
    basicTestTimedWait(apps);
    waitForTestThreads();
  }

  private void basicTestTimedWait(RestartUnitTestApp[] apps) throws Exception {
    ServerControl server = env.getServer();
    Object lck = new Object();
    AppStateSnapshot snapshot = new AppStateSnapshot(apps);
    RestartUnitTestApp shutdownBlocker = newTestApps(1)[0];
    shutdownBlocker.setStartBarrier(new CyclicBarrier(1));
    final CyclicBarrier startBarrier = new CyclicBarrier(apps.length + 1);
    apps[0].setDistributedSharedLock(lck);
    final int waitTime = 10 * 1000;
    for (int i = 0; i < apps.length; i++) {
      final RestartUnitTestApp app = apps[i];
      app.setID(i);
      app.reset();
      app.setStartBarrier(startBarrier);
      env.startNewClientThread(new Runnable() {
        public void run() {
          app.doWait(waitTime);
        }
      });
    }

    startBarrier.barrier();
    while (!snapshot.takeSnapshot().allWaiters()) {
      System.out.println("Waiting for all apps to be waiters: " + snapshot);
      ThreadUtil.reallySleep(100);
    }
    System.out.println("obtaining a lock to block shutdown...");
    FutureResult shutdownBlockerBarrier = new FutureResult();
    shutdownBlocker.blockShutdown(shutdownBlockerBarrier);
    System.out.println("starting server shutdown sequence...");
    server.attemptForceShutdown();
    System.out.println("waiting for timers to go off in server before allowing shutdown to complete...");
    ThreadUtil.reallySleep(waitTime + 1000);
    System.out.println("letting the server finish shutting down...");
    shutdownBlockerBarrier.set(new Object());
    System.out.println("waiting until server is actually shut down...");
    server.waitUntilShutdown();

    assertTrue(snapshot.takeSnapshot().allWaiters());
    ThreadUtil.reallySleep(waitTime);
    assertTrue(snapshot.takeSnapshot().allWaiters());
    server.start();
    while (!snapshot.takeSnapshot().allEnded()) {
      System.out.println(snapshot);
      ThreadUtil.reallySleep(500);
    }
  }

  private void basicTestWaitNotifyAll(RestartUnitTestApp[] apps) throws Exception {
    ServerControl server = env.getServer();
    Object lck = new Object();
    AppStateSnapshot snapshot = new AppStateSnapshot(apps);
    CyclicBarrier startBarrier = new CyclicBarrier(1);
    apps[0].setDistributedSharedLock(lck);
    for (int i = 0; i < apps.length; i++) {
      final RestartUnitTestApp app = apps[i];
      app.setID(i);
      app.reset();
      app.setStartBarrier(startBarrier);
      env.startNewClientThread(new Runnable() {
        public void run() {
          app.doWait(0);
        }
      });
    }
    // wait until all the threads have waited on the object.
    while (!snapshot.allWaiters()) {
      snapshot.takeSnapshot();
    }

    server.shutdown();
    final CyclicBarrier serverStartBarrier = new CyclicBarrier(2);
    final RestartUnitTestApp app = apps[0];
    new Thread(new Runnable() {
      public void run() {
        try {
          serverStartBarrier.barrier();
          app.doNotifyAll();
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    }).start();

    serverStartBarrier.barrier();
    server.start();
    do {
      snapshot.takeSnapshot();
      System.out.println(snapshot);
      Thread.sleep(500);
    } while (!snapshot.allEnded());
  }

  private RestartUnitTestApp newRestartTestApp(ThreadGroup tg) throws Exception {
    // FIXME 2005-12-01 andrew
    // IsolationClassLoader cl = new IsolationClassLoader(env.getDSOConfig(), env.getL1DSOConfig());
    // Class clazz = cl.loadClass(DSOServerRestartTestApp.class.getName());
    // Constructor ctor = clazz.getConstructor(new Class[] { ThreadGroup.class });
    // return (RestartUnitTestApp) ctor.newInstance(new Object[] { tg });
    return null;
  }

  private RestartUnitTestApp[] newTestApps(int instanceCount) throws Exception {
    // FIXME 2005-12-01 andrew
    // DSOServerRestartTestApp.visitL1DSOConfig(null, env.getL1DSOConfig());
    RestartUnitTestApp[] apps = new RestartUnitTestApp[instanceCount];
    for (int i = 0; i < apps.length; i++) {
      apps[i] = newRestartTestApp(env.getThreadGroup());
    }
    return apps;
  }

  public void testServerStartAndStop() throws Exception {
    env.newExtraProcessServer();
    assertTrue(isEmptyDirectory(env.getDBHome()));
    env.startServer(startTimeout);
    env.shutdownServer();
    assertFalse(isEmptyDirectory(env.getDBHome()));
  }

  private boolean isEmptyDirectory(File dir) {
    if (dir.exists()) {
      if (!dir.isDirectory()) return false;
      if (dir.listFiles().length != 0) return false;
    }
    return true;
  }

  private void waitForTestThreads() {
    while (!env.hasThreadGroupErrors() && env.hasActiveClients()) {
      ThreadUtil.reallySleep(500);
    }
  }

  private void assertNoTestThreadExceptions() {
    boolean failed = false;
    for (Iterator i = env.getThreadGroupErrors().iterator(); i.hasNext();) {
      ((Throwable) i.next()).printStackTrace();
      failed = true;
    }
    assertFalse("There were test thread exceptions", failed);
  }
}
