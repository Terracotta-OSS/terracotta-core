/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

      ensureUnlockedLocksCanBeCollected(true);
      ensureUnlockedLocksCanBeCollected(false);
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

    private WeakReference<Lock>[] lockAndWeaklyRef(boolean read) {
      WeakReference<Lock>[] refs = new WeakReference[SIZE * 2];

      for (int i = 0, j = 0; j < refs.length; i++, j += 2) {

        Lock lock = locks[i];
        Lock rw = read ? readLocks[i] : writeLocks[i];

        lock.lock();
        rw.lock();

        refs[j] = new WeakReference<Lock>(lock);
        refs[j + 1] = new WeakReference<Lock>(rw);
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
      for (int i = 0; i < refs.length; i++) {
        refs[i].get().unlock();
      }
    }

    private void assertRefs(WeakReference<Lock>[] refs, boolean notNull) {
      for (int i = 0; i < refs.length; i++) {
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
      TCObject tco = ManagerUtil.getObject(locks);
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
