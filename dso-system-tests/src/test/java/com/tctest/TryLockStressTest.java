/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TryLockStressTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final int  NUM_LOCKS     = 10;
    private static final int  NUM_THREADS   = 5;
    private static final int  LOCK_RATIO    = 5;
    private static final long LOAD_DURATION = 3 * 60 * 1000;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      List locks = makeLocks();

      LockerThread[] threads = new LockerThread[NUM_THREADS];

      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i] = new LockerThread(locks);
        threads[i].start();
      }

      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i].finish();
      }
    }

    private List makeLocks() {
      List locks = new ArrayList();
      for (int i = 0; i < NUM_LOCKS; i++) {
        locks.add("lock" + i);
      }
      return locks;
    }

    private static class LockerThread extends Thread {

      private final SynchronizedRef error  = new SynchronizedRef(null);
      private final List            locks;
      private final Random          random = new Random();

      private boolean lockSucceeded = false;
      private boolean tryLockSucceeded = false;
      private boolean tryLockFailed = false;
      private boolean tryLockTimeoutSucceeded = false;
      private boolean tryLockTimeoutFailed = false;

      LockerThread(List locks) {
        this.locks = locks;
        setDaemon(true);
      }

      public void run() {
        try {
          run0();
        } catch (Throwable t) {
          t.printStackTrace();
          error.set(t);
        }
      }

      private void run0() throws Exception {
        final long end = System.currentTimeMillis() + LOAD_DURATION;
        while (System.currentTimeMillis() < end) {
          String lock = (String) locks.get(random.nextInt(NUM_LOCKS));
          int lock_mode = random.nextInt(LOCK_RATIO);
          if (0 == lock_mode) {
            ManagerUtil.beginLock(lock, Manager.LOCK_TYPE_WRITE);
            try {
              lockSucceeded = true;
              ThreadUtil.reallySleep(random.nextInt(5));
            } finally {
              ManagerUtil.commitLock(lock, Manager.LOCK_TYPE_WRITE);
            }
          } else if (1 == lock_mode) {
            if (ManagerUtil.tryBeginLock(lock, Manager.LOCK_TYPE_WRITE, random.nextInt(20)*1000L)) {
              try {
                tryLockTimeoutSucceeded = true;
                ThreadUtil.reallySleep(random.nextInt(5));
              } finally {
                ManagerUtil.commitLock(lock, Manager.LOCK_TYPE_WRITE);
              }
            } else {
              tryLockTimeoutFailed = true;
            }
          } else {
            if (ManagerUtil.tryBeginLock(lock, Manager.LOCK_TYPE_WRITE)) {
              try {
                tryLockSucceeded = true;
                ThreadUtil.reallySleep(random.nextInt(5));
              } finally {
                ManagerUtil.commitLock(lock, Manager.LOCK_TYPE_WRITE);
              }
            } else {
              tryLockFailed = true;
            }
          }
        }
      }

      void finish() throws Throwable {
        join();
        Throwable t = (Throwable) error.get();
        if (t != null) { throw t; }
        Assert.assertTrue(lockSucceeded);
        Assert.assertTrue(tryLockTimeoutSucceeded);
        Assert.assertTrue(tryLockTimeoutFailed);
        Assert.assertTrue(tryLockSucceeded);
        Assert.assertTrue(tryLockFailed);
      }
    }
  }

}