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
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TryLockStressTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
//  private static final int NODE_COUNT = 3;

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
          if (random.nextInt(LOCK_RATIO) == 0) {
            ManagerUtil.beginLock(lock, Manager.LOCK_TYPE_WRITE);
            try {
              ThreadUtil.reallySleep(random.nextInt(5));
            } finally {
              ManagerUtil.commitLock(lock);
            }
          } else {
            if (ManagerUtil.tryBeginLock(lock, Manager.LOCK_TYPE_WRITE)) {
              try {
                ThreadUtil.reallySleep(random.nextInt(5));
              } finally {
                ManagerUtil.commitLock(lock);
              }
            }
          }
        }
      }

      void finish() throws Throwable {
        join();
        Throwable t = (Throwable) error.get();
        if (t != null) { throw t; }
      }
    }

  }

}