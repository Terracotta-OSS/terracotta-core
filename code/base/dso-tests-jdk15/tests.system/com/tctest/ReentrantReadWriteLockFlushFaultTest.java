/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.TCObject;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private static final long                     SLEEP    = 2000;
    private static final int                      SIZE     = 8;
    private static final int                      DURATION = 1000 * 60 * 3;

    private static final ReentrantReadWriteLock[] locks    = new ReentrantReadWriteLock[SIZE];

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      // NOTE: It is important to not have any (non dso managed) strong references the the shared locks in this test
      // If you did, they would not get flushed/faulted defeating the purpose of this test

      initLocks();

      useLocksAndClearRefs();

      ensureUnlockedLocksCanBeCollected(true);
      ensureUnlockedLocksCanBeCollected(false);
    }

    private void initLocks() {
      synchronized (locks) {
        for (int i = 0; i < SIZE; i++) {
          locks[i] = new ReentrantReadWriteLock();
        }
      }
    }

    private WeakReference<Lock>[] lockAndWeaklyRef(boolean read) {
      WeakReference<Lock>[] refs = new WeakReference[SIZE];

      for (int i = 0; i < SIZE; i++) {
        final Lock lock;
        if (read) {
          lock = locks[i].readLock();
        } else {
          lock = locks[i].writeLock();
        }
        lock.lock();
        refs[i] = new WeakReference<Lock>(lock);
      }

      return refs;
    }

    private void ensureUnlockedLocksCanBeCollected(boolean read) {
      WeakReference<Lock>[] refs = lockAndWeaklyRef(read);

      System.gc();
      ThreadUtil.reallySleep(SLEEP);
      System.gc();
      ThreadUtil.reallySleep(SLEEP);

      assertRefs(refs, true);

      unlockLockRefs(refs);

      clearDsoManagedReferences();

      assertRefs(refs, false);
    }

    private void unlockLockRefs(WeakReference<Lock>[] refs) {
      for (int i = 0; i < SIZE; i++) {
        refs[i].get().unlock();
      }
    }

    private void assertRefs(WeakReference<Lock>[] refs, boolean notNull) {
      for (int i = 0; i < SIZE; i++) {
        if (notNull) {
          Assert.assertNotNull("ref " + i, refs[i].get());
        } else {
          Assert.assertNull("ref " + i, refs[i].get());
        }
      }
    }

    private void useLocksAndClearRefs() {
      Random random = new Random();

      final long end = System.currentTimeMillis() + DURATION;

      int last = -1;
      boolean read = false;

      while (System.currentTimeMillis() < end) {
        if (last != -1) {
          if (read) {
            Assert.assertEquals(1, locks[last].getReadLockCount());
            locks[last].readLock().unlock();
          } else {
            Assert.assertTrue(locks[last].isWriteLocked());
            Assert.assertTrue(locks[last].isWriteLockedByCurrentThread());
            Assert.assertEquals(1, locks[last].getWriteHoldCount());
            locks[last].writeLock().unlock();
          }
        }

        last = random.nextInt(SIZE);
        read = random.nextBoolean();

        if (read) {
          locks[last].readLock().lock();
        } else {
          locks[last].writeLock().lock();
        }

        clearDsoManagedReferences();
      }

      // undo the last lock
      if (read) {
        locks[last].readLock().unlock();
      } else {
        locks[last].writeLock().unlock();
      }
    }

    private void clearDsoManagedReferences() {
      TCObject tco = ManagerUtil.getObject(locks);

      tco.clearAccessed();
      tco.clearReferences(SIZE);

      System.gc();
      ThreadUtil.reallySleep(SLEEP);

      System.gc();
      ThreadUtil.reallySleep(SLEEP);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      TransparencyClassSpec spec = config.getOrCreateSpec(App.class.getName());
      spec.addRoot("locks", "locks");

      // config.addIncludePattern(ClassWithAnnotatedRoot.class.getName());
      config.addWriteAutolock("* " + App.class.getName() + "*.*(..)");

    }

  }

}
