/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockDowngraderTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;
  private static final int LOOP_COUNT = 1000;

  @Override
  protected Class getApplicationClass() {
    return ReentrantReadWriteLockDowngraderTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  public static class ReentrantReadWriteLockDowngraderTestApp extends AbstractErrorCatchingTransparentApp {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    // private final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(true);
    private final Map                    map  = new HashMap();
    private final CyclicBarrier          barrier;

    public ReentrantReadWriteLockDowngraderTestApp(String appId, ApplicationConfig cfg,
                                                   ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = ReentrantReadWriteLockDowngraderTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(testClass + "$*");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("lock", "lock");
      // spec.addRoot("lock2", "lock2");
      spec.addRoot("barrier", "barrier");
      spec.addRoot("map", "map");
    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();
      // lock2.writeLock().lock();
      // lock2.readLock().lock();
      // lock2.writeLock().unlock();
      switch (index) {
        case 0:
          write();
          break;
        case 1:
          read();
          break;
        case 2:
          downgrade();
          break;
        default:
          throw new AssertionError("Shouldn't have come here : " + index);
      }
      // lock2.readLock().unlock();
      barrier.await();
    }

    private void read() {
      println("Reader started");
      for (int i = 0; i < LOOP_COUNT; i++) {
        lock.readLock().lock();
        try {
          map.get(new Integer(i));
        } finally {
          lock.readLock().unlock();
        }
      }
      println("Reader completed");
    }

    private static void println(String msg) {
      System.err.println(Thread.currentThread().getName() + " : " + msg);
    }

    private void write() {
      println("Writer started");
      for (int i = 0; i < LOOP_COUNT; i++) {
        lock.writeLock().lock();
        try {
          map.put(new Integer(i), new Long(i));
        } finally {
          lock.writeLock().unlock();
        }
      }
      println("Writer completed");
    }

    private void downgrade() {
      println("Downgrader started");
      for (int i = LOOP_COUNT; i < LOOP_COUNT * 2; i++) {
        lock.writeLock().lock();
        try {
          map.put(new Integer(i), new Long(i));
        } finally {
          // Downgrade
          lock.readLock().lock();
          lock.writeLock().unlock();
        }
        try {
          Object o = map.get(new Integer(i));
          assertNotNull(o);
        } finally {
          lock.readLock().unlock();
        }
      }
      println("Downgrader completed");
    }

  }

}
