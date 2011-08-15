/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ReentrantReadWriteLockFlushFaultTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class<App> getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final long SLEEP      = 2000;
    private static final int  SIZE       = 8;
    private static final int  DURATION   = 1000 * 60 * 3;

    private final WriteLock[] writeLocks = new WriteLock[SIZE];
    private final ReadLock[]  readLocks  = new ReadLock[SIZE];
    private final Lock[]      locks      = new Lock[SIZE];

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      // NOTE: It is important to not have any (non dso managed) strong references the the shared locks in this test
      // If you did, they would not get flushed/faulted defeating the purpose of this test

      initLocks();

      useLocksAndClearRefs();
    }

    private void initLocks() {
      synchronized (locks) {
        for (int i = 0; i < SIZE; i++) {
          locks[i] = new ReentrantLock();

          ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
          readLocks[i] = rwLock.readLock();
          writeLocks[i] = rwLock.writeLock();
        }
      }
    }

    private void useLocksAndClearRefs() {
      Random random = new Random();

      final long end = System.currentTimeMillis() + DURATION;

      int last = -1;
      boolean read = false;

      while (true) {
        if (last != -1) {
          if (read) {
            readLocks[last].unlock();
          } else {
            writeLocks[last].unlock();
          }
          locks[last].unlock();
        }

        if (System.currentTimeMillis() > end) {
          break;
        }

        last = random.nextInt(SIZE);
        read = random.nextBoolean();

        if (read) {
          readLocks[last].lock();
        } else {
          writeLocks[last].lock();
        }
        locks[last].lock();

        clearDsoManagedReferences();
      }
    }

    private void clearDsoManagedReferences() {
      clearRefs(locks);
      clearRefs(readLocks);
      clearRefs(writeLocks);

      System.gc();
      ThreadUtil.reallySleep(SLEEP);

      System.gc();
      ThreadUtil.reallySleep(SLEEP);
    }

    private static void clearRefs(Object[] locks) {
      TCObjectExternal tco = ManagerUtil.getObject(locks);
      tco.clearAccessed();
      tco.clearReferences(SIZE);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      TransparencyClassSpec spec = config.getOrCreateSpec(App.class.getName());
      spec.addRoot("locks", "locks");
      spec.addRoot("writeLocks", "writeLocks");
      spec.addRoot("readLocks", " readLocks");
      config.addWriteAutolock("* " + App.class.getName() + "*.*(..)");
    }

  }

}
