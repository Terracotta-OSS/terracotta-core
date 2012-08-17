/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import junit.framework.Assert;

public class TryLockStressTestClient extends ClientBase {

  private static final int  NUM_LOCKS     = 10;
  private static final int  NUM_THREADS   = 5;
  private static final int  LOCK_RATIO    = 5;
  private static final long LOAD_DURATION = 3 * 60 * 1000;

  public TryLockStressTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Exception {
    List locks = makeLocks(toolkit);

    TryLockStressTestClient.LockerThread[] threads = new TryLockStressTestClient.LockerThread[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i] = new TryLockStressTestClient.LockerThread(locks);
      threads[i].start();
    }

    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i].finish();
    }
  }

  private List makeLocks(Toolkit toolkit) {
    List locks = new ArrayList();
    for (int i = 0; i < NUM_LOCKS; i++) {
      locks.add(toolkit.getReadWriteLock("lock" + i).writeLock());
    }
    return locks;
  }

  private static class LockerThread extends Thread {

    private Exception    error;
    private final List   locks;
    private final Random random                  = new Random();

    private boolean      lockSucceeded           = false;
    private boolean      tryLockSucceeded        = false;
    private boolean      tryLockFailed           = false;
    private boolean      tryLockTimeoutSucceeded = false;
    private boolean      tryLockTimeoutFailed    = false;

    private LockerThread(List locks) {
      this.locks = locks;
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        run0();
      } catch (Exception t) {
        t.printStackTrace();
        error = t;
      }
    }

    private void run0() throws Exception {
      final long end = System.currentTimeMillis() + LOAD_DURATION;
      while (System.currentTimeMillis() < end) {
        Lock lock = (Lock) locks.get(random.nextInt(NUM_LOCKS));
        int lock_mode = random.nextInt(LOCK_RATIO);
        if (0 == lock_mode) {
          lock.lock();
          try {
            lockSucceeded = true;
            ThreadUtil.reallySleep(random.nextInt(5));
          } finally {
            lock.unlock();
          }
        } else if (1 == lock_mode) {
          if (lock.tryLock(random.nextInt(20), TimeUnit.SECONDS)) {
            try {
              tryLockTimeoutSucceeded = true;
              ThreadUtil.reallySleep(random.nextInt(5));
            } finally {
              lock.unlock();
            }
          } else {
            tryLockTimeoutFailed = true;
          }
        } else {
          if (lock.tryLock()) {
            try {
              tryLockSucceeded = true;
              ThreadUtil.reallySleep(random.nextInt(5));
            } finally {
              lock.unlock();
            }
          } else {
            tryLockFailed = true;
          }
        }
      }
    }

    void finish() throws Exception {
      join();
      if (error != null) { throw error; }
      Assert.assertTrue(lockSucceeded);
      Assert.assertTrue(tryLockTimeoutSucceeded);
      Assert.assertTrue(tryLockTimeoutFailed);
      Assert.assertTrue(tryLockSucceeded);
      Assert.assertTrue(tryLockFailed);
    }
  }
}