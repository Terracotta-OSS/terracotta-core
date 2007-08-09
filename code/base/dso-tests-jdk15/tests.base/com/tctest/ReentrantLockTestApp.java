/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTestApp extends AbstractTransparentApp {
  public static final String  CRASH_TEST         = "CRASH_TEST";

  private static final int    NUM_OF_PUTS        = 500;
  private static final int    NUM_OF_LOOPS       = 5;

  private final DataRoot      root               = new DataRoot();
  private final List          queue              = new LinkedList();
  private final CyclicBarrier barrier2           = new CyclicBarrier(2);
  private final CyclicBarrier barrier3           = new CyclicBarrier(3);
  private final CyclicBarrier barrier;

  private final Random        random;

  private final ReentrantLock unsharedUnfairLock = new ReentrantLock();
  private final ReentrantLock unsharedFairLock   = new ReentrantLock(true);

  private final Object        testLockObject     = new Object();

  private int                 numOfPutters       = 1;
  private int                 numOfGetters;
  private final boolean       isCrashTest;

  public ReentrantLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());
    numOfGetters = getParticipantCount() - numOfPutters;
    isCrashTest = "true".equals(cfg.getAttribute(CRASH_TEST))? true : false;
  }

  // TODO: We need to add a test case for a situation where an unshared ReentrantLock become
  // shared.
  public void run() {
    try {
      barrier.await();

      sharedUnSharedTesting();
      multipleReentrantLocksTesting();
      singleNodeTryBeginLockTesting();
      variousLockUnLockPatternTesting();

      System.err.println("Testing unfair lock ...");

      basicConditionVariableTesting(root.getUnfairLock(), root.getUnfairCondition());
      basicConditionVariableWaitTesting(root.getUnfairLock(), root.getUnfairCondition());
      basicUnsharedLockTesting(unsharedUnfairLock);
      for (int i = 0; i < NUM_OF_LOOPS; i++) {
        basicLockTesting(root.getUnfairLock());
      }
      lockSyncLockTesting(root.getUnfairLock());

      tryLockTimeoutSingleNodeTesting(root.getUnfairLock());
      tryLockTimeoutMultiNodesTesting(root.getUnfairLock());
      tryLockTesting(root.getUnfairLock());

      threadInterruptedLockTesting(root.getUnfairLock());

      System.err.println("Testing fair lock ...");

      basicConditionVariableTesting(root.getFairLock(), root.getFairCondition());
      basicConditionVariableWaitTesting(root.getFairLock(), root.getFairCondition());
      basicUnsharedLockTesting(unsharedFairLock);
      for (int i = 0; i < NUM_OF_LOOPS; i++) {
        basicLockTesting(root.getFairLock());
      }
      lockSyncLockTesting(root.getFairLock());

      tryLockTimeoutSingleNodeTesting(root.getFairLock());
      tryLockTimeoutMultiNodesTesting(root.getFairLock());
      tryLockTesting(root.getFairLock());

      threadInterruptedLockTesting(root.getFairLock());

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void sharedUnSharedTesting() throws Exception {
    int index = barrier.await();

    if (index == 0) {
      ReentrantLock lock = new ReentrantLock();
      Thread thread = new Thread(new TestRunnable2(lock));
      lock.lock();
      thread.start();
      synchronized (root) {
        root.setLazyLock(lock);
      }
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(root.getLazyLock().isLocked());
      root.getLazyLock().lock();
    }

    if (index == 0) {
      root.getLazyLock().unlock();
      root.getLazyLock().unlock();
    }

    barrier2.await();

    barrier.await();

    // Now, make sure another node can lock the lock.
    if (index == 1) {
      root.getLazyLock().lock();
      root.getLazyLock().lock();
    }

    if (index == 1) {
      root.getLazyLock().unlock();
      root.getLazyLock().unlock();
    }

    barrier.await();

    if (index == 1) {
      Assert.assertFalse(root.getLazyLock().isLocked());
    }

    barrier.await();

    if (index == 0) {
      final ReentrantLock lock = new ReentrantLock();
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread thread = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = lock.tryLock(8000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(isLocked);
            lock.unlock();
            localBarrier.await();
          } catch (Exception ie) {
            throw new AssertionError(ie);
          }
        }
      });
      lock.lock();
      thread.start();
      synchronized (root) {
        root.setLazyLock(lock);
      }
      lock.unlock();

      localBarrier.await();

    }

    barrier.await();

    if (index == 0) {
      Assert.assertFalse(root.getLazyLock().isLocked());
    }

    barrier.await();
  }

  /**
   * This testcase is to ensure that the unlocking sequence can be different from the locking sequence. For example,
   * lock1.lock(), lock2.lock(), lock1.unlock(), and lock2.unlock() as well as lock1.lock(), lock2.lock(),
   * lock2.unlock(), lock1.unlock() will both work correctly.
   */
  private void multipleReentrantLocksTesting() throws Exception {
    int index = barrier.await();

    if (index == 0) {
      root.getUnfairLock().lock();
      root.getFairLock().lock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
      Assert.assertTrue(root.getFairLock().isLocked());
    }

    barrier.await();

    if (index == 0) {
      root.getFairLock().unlock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
      Assert.assertFalse(root.getFairLock().isLocked());
    }

    barrier.await();

    if (index == 0) {
      root.getUnfairLock().unlock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertFalse(root.getUnfairLock().isLocked());
      Assert.assertFalse(root.getFairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getUnfairLock().lock();
      root.getFairLock().lock();
    }

    barrier.await();

    if (index == 1) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
      Assert.assertTrue(root.getFairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getUnfairLock().unlock();
    }

    barrier.await();

    if (index == 1) {
      Assert.assertFalse(root.getUnfairLock().isLocked());
      Assert.assertTrue(root.getFairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getFairLock().unlock();
    }

    barrier.await();

    if (index == 1) {
      Assert.assertFalse(root.getUnfairLock().isLocked());
      Assert.assertFalse(root.getFairLock().isLocked());
    }

    barrier.await();
  }

  private void singleNodeTryBeginLockTesting() throws Exception {
    int index = barrier.await();

    if (index == 0) {
      String lockId = "testLock";
      CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread thread1 = new Thread(new TestTryLockFailRunnable(lockId, localBarrier));

      ManagerUtil.beginLock(lockId, LockLevel.WRITE);
      thread1.start();
      localBarrier.await();
      ManagerUtil.commitLock(lockId);
    }

    barrier.await();
  }

  /**
   * This testcase is to ensure that various lock and unlock sequence are functioned correctly. For example, lock() and
   * unlock() method can be invoked within a synchronized block.
   */
  private void variousLockUnLockPatternTesting() throws Exception {
    int index = barrier.await();

    if (index == 0) {
      root.getUnfairLock().lock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
    }

    barrier.await();

    if (index == 0) {
      synchronized (testLockObject) {
        root.getUnfairLock().unlock();
      }
    }

    barrier.await();

    if (index == 0) {
      Assert.assertFalse(root.getUnfairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getUnfairLock().lock();
      synchronized (testLockObject) {
        root.getUnfairLock().lock();
      }
    }

    barrier.await();

    if (index == 1) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getUnfairLock().unlock();
    }

    barrier.await();

    if (index == 1) {
      Assert.assertTrue(root.getUnfairLock().isLocked());
    }

    barrier.await();

    if (index == 1) {
      root.getUnfairLock().unlock();
    }

    barrier.await();

    /*
     * Thread thread = new Thread(new TestRunnable3(root.getUnfairLock(), testLockObject)); if (index == 0) {
     * thread.start(); } barrier.await(); if (index == 0) { Assert.assertTrue(root.getUnfairLock().isLocked()); }
     * barrier.await(); if (index == 0) { thread.interrupt(); } barrier.await();
     */
  }

  private void basicConditionVariableWaitTesting(ReentrantLock lock, Condition condition) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      Thread thread = new Thread(new TestRunnable1(lock, condition));
      thread.start();
    }

    barrier.await();

    if (index == 0) {
      Thread.sleep(1000); // Sleep so that the TestRunnable1 thread can pick up.
      Assert.assertEquals(1, lock.getWaitQueueLength(condition));
    }

    barrier.await();

    if (index == 0) {
      lock.lock();
      try {
        condition.signalAll();
      } finally {
        lock.unlock();
      }
    }

    barrier.await();

    if (index == 0) {
      Assert.assertEquals(0, lock.getWaitQueueLength(condition));
    }

    barrier.await();
  }

  /**
   * This test case tests the condition variable API. A condition variable is returned by called the newCondition()
   * method of an reentrant lock.
   */
  private void basicConditionVariableTesting(ReentrantLock lock, Condition condition) throws Exception {
    int index = barrier.await();

    final long id = new Long(getApplicationId()).longValue();

    if (index == 2) {
      doPutter(id, lock, condition);
    } else {
      doGetter(id, lock, condition);
    }

    barrier.await();
  }

  private void doPutter(long id, ReentrantLock lock, Condition condition) throws Exception {
    Thread.currentThread().setName("PUTTER-" + id);

    for (int i = 0; i < NUM_OF_PUTS; i++) {
      lock.lock();
      try {
        System.err.println("PUTTER-" + id + " Putting " + i);
        queue.add(new WorkItem(String.valueOf(i)));
        if (i % 2 == 0) {
          condition.signalAll();
        } else {
          condition.signal();
        }
      } finally {
        lock.unlock();
      }
    }

    for (int i = 0; i < numOfGetters; i++) {
      lock.lock();
      try {
        queue.add(WorkItem.STOP);
        condition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  private void doGetter(long id, ReentrantLock lock, Condition condition) throws Exception {
    Thread.currentThread().setName("GETTER-" + id);

    int i = 0;
    while (true) {
      lock.lock();
      lock.lock();
      try {
        while (queue.size() == 0) {
          int choice = i % 4;
          switch (choice) {
            case 0:
              condition.await();
              break;
            case 1:
              condition.awaitUninterruptibly();
              break;
            case 2:
              long millis = random.nextInt(10000);
              condition.await(millis, TimeUnit.MILLISECONDS);
              break;
            case 3:
              long nanos = random.nextInt(10000);
              condition.awaitNanos(nanos);
              break;
          }
          i++;
        }
        WorkItem wi = (WorkItem) queue.remove(0);
        if (wi.isStop()) { return; }
        System.err.println("GETTER- " + id + " removes " + wi);

      } finally {
        lock.unlock();
        lock.unlock();
      }
    }
  }

  /**
   * This test case makes sure that an unshared reentrant lock is functioning as normal.
   */
  private void basicUnsharedLockTesting(ReentrantLock unsharedLock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      unsharedLock.lock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(unsharedLock.isLocked());
    } else {
      Assert.assertFalse(unsharedLock.isLocked());
    }

    barrier.await();

    if (index != 0) {
      boolean haveLock = unsharedLock.tryLock();
      Assert.assertTrue(haveLock);
    }

    barrier.await();

    if (index == 0) {
      Assert.assertTrue(unsharedLock.isLocked());
    } else {
      Assert.assertTrue(unsharedLock.isLocked());
    }

    barrier.await();

    if (index == 0) {
      unsharedLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertFalse(unsharedLock.isLocked());
    } else {
      Assert.assertTrue(unsharedLock.isLocked());
    }

    barrier.await();

    if (index != 0) {
      unsharedLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      Assert.assertFalse(unsharedLock.isLocked());
    } else {
      Assert.assertFalse(unsharedLock.isLocked());
    }

    barrier.await();
  }

  /**
   * This test case tests if tryLock() will throw an InterruptedException when the thread is interrupted.
   */

  private void threadInterruptedLockTesting(ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      lock.lock();
    }
    barrier.await();
    Thread thread = new Thread(new InterruptedRunnable(lock));
    if (index == 0) {
      thread.start();
    }
    barrier.await();
    if (index == 0) {
      thread.interrupt();
      lock.unlock();
    }
    barrier.await();
  }

  /**
   * This test case makes sure that data modifications within lock() and unlock() are reflected in the other client.
   */
  private void lockSyncLockTesting(ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      lock.lock();
      try {
        root.setData(10);
      } finally {
        lock.unlock();
      }
    }

    barrier.await();

    Assert.assertEquals(10, root.getData());

    barrier.await();
  }

  private void tryLockTesting(final ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 1) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final CyclicBarrier threadBarrier = new CyclicBarrier(2);

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          lock.lock();
          try {
            threadBarrier.await();
            threadBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            lock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            threadBarrier.await();
            boolean isLocked = lock.tryLock();
            threadBarrier.await();
            Assert.assertFalse(isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final CyclicBarrier threadBarrier = new CyclicBarrier(2);
      final ReentrantLock nonSharedLock = new ReentrantLock();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          nonSharedLock.lock();
          try {
            threadBarrier.await();
            threadBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            nonSharedLock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            threadBarrier.await();
            boolean isLocked = nonSharedLock.tryLock();
            threadBarrier.await();
            Assert.assertFalse(isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      final ReentrantLock nonSharedLock = new ReentrantLock();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = nonSharedLock.tryLock();
            Assert.assertTrue(isLocked);
            nonSharedLock.unlock();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      localBarrier.await();
    }

    barrier.await();

  }

  private void tryLockTimeoutMultiNodesTesting(final ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            barrier3.await();
            lock.lock();
            Thread.sleep(2000);
            lock.unlock();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            barrier3.await();
            boolean isLocked = lock.tryLock(70, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { lock.unlock(); }
          } catch (Exception e) {
            throw new AssertionError(e);
          }

          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
      barrier.await();

    } else if (index == 1) {
      lock.lock();
      try {
        barrier3.await();
        Thread.sleep(1000);
      } finally {
        lock.unlock();
      }
      lock.lock();
      try {
        Thread.sleep(1000);
      } finally {
        lock.unlock();
      }
      barrier.await();
    } else {
      barrier.await();
    }

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          lock.lock();
          try {
            barrier2.await();
            Thread.sleep(1000);
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            lock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = lock.tryLock(9000, TimeUnit.MILLISECONDS);
            // boolean isLocked = lock.tryLock(9000, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { lock.unlock(); }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();

      barrier.await();
    } else if (index == 1) {
      barrier2.await();
      boolean isLocked = lock.tryLock(4001, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      if (isLocked) { lock.unlock(); }

      barrier.await();
    } else {
      barrier.await();
    }

    if (index == 0) {
      lock.lock();
      try {
        Thread.sleep(1000);
      } finally {
        lock.unlock();
      }
      barrier.await();
    } else if (index == 1) {
      boolean isLocked = lock.tryLock(4000, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      if (isLocked) { lock.unlock(); }

      barrier.await();
    } else {
      barrier.await();
    }

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          lock.lock();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          } finally {
            lock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = lock.tryLock(9001, TimeUnit.MILLISECONDS);
            // boolean isLocked = lock.tryLock(60, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { lock.unlock(); }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();

      barrier.await();
    } else if (index == 1) {
      boolean isLocked = lock.tryLock(4001, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      if (isLocked) { lock.unlock(); }

      barrier.await();
    } else {
      barrier.await();
    }

  }

  private void tryLockTimeoutSingleNodeTesting(final ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 1) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          lock.lock();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          } finally {
            lock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = lock.tryLock(4002, TimeUnit.MILLISECONDS);
            // boolean isLocked = lock.tryLock(60, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { lock.unlock(); }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final ReentrantLock nonSharedLock = new ReentrantLock();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          nonSharedLock.lock();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          } finally {
            nonSharedLock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = nonSharedLock.tryLock(5, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { nonSharedLock.unlock(); }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final ReentrantLock nonSharedLock = new ReentrantLock();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          nonSharedLock.lock();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          } finally {
            nonSharedLock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            boolean isLocked = nonSharedLock.tryLock(TimeUnit.MICROSECONDS.convert(6, TimeUnit.SECONDS),
                                                     TimeUnit.MICROSECONDS);
            assertTryLockResult(isLocked);
            if (isLocked) { nonSharedLock.unlock(); }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final CyclicBarrier threadBarrier = new CyclicBarrier(2);
      final ReentrantLock nonSharedLock = new ReentrantLock();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          nonSharedLock.lock();
          try {
            threadBarrier.await();
            threadBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            nonSharedLock.unlock();
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            threadBarrier.await();
            boolean isLocked = nonSharedLock.tryLock(10, TimeUnit.MICROSECONDS);
            threadBarrier.await();
            assertTryLockResult(!isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();
  }

  /**
   * This test case provides basic testing to the API of an reentrant lock when an reentrant lock is shared.
   */
  private void basicLockTesting(ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      lock.lock();
      boolean isLocked = lock.tryLock();

      Assert.assertTrue(isLocked);

      isLocked = lock.tryLock(300000L, TimeUnit.NANOSECONDS);

      Assert.assertTrue(isLocked);

      lock.unlock();

      try {
        lock.hasQueuedThread(Thread.currentThread());
        throw new AssertionError("Should have thrown an TCNotSupportedMethodException.");
      } catch (TCNotSupportedMethodException e) {
        // Expected
      }
    }
    barrier.await();

    if (index != 0) {
      boolean haveLocked = lock.tryLock();
      Assert.assertFalse(haveLocked);

      haveLocked = lock.tryLock(300000L, TimeUnit.NANOSECONDS);
      Assert.assertFalse(haveLocked);
    }

    barrier.await();

    if (index == 0) {
      Assert.assertEquals(2, lock.getHoldCount());
    } else {
      Assert.assertEquals(0, lock.getHoldCount());
    }

    Assert.assertEquals(0, lock.getQueueLength());

    Assert.assertFalse(lock.hasQueuedThreads());

    if (index == 0) {
      Assert.assertTrue(lock.isHeldByCurrentThread());
    } else {
      Assert.assertFalse(lock.isHeldByCurrentThread());
    }
    Assert.assertTrue(lock.isLocked());

    barrier.await();

    if (index == 0) {
      lock.unlock();
    }

    barrier.await();

    Assert.assertTrue(lock.isLocked());

    barrier.await();

    if (index == 0) {
      lock.unlock();
    }

    barrier.await();

    Assert.assertEquals(0, lock.getQueueLength());
    Assert.assertFalse(lock.isLocked());

    barrier.await();
  }
  
  private void assertTryLockResult(boolean isLocked) {
    if (!isCrashTest) {
      Assert.assertTrue(isLocked);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReentrantLockTestApp.class.getName();
    ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("barrier3", "barrier3");
    spec.addRoot("queue", "queue");
    spec.addRoot("testLockObject", "testLockObject");
  }

  private static class TestRunnable1 implements Runnable {
    private ReentrantLock lock;
    private Condition     conditionObject;

    public TestRunnable1(ReentrantLock lock, Condition conditionObject) {
      this.lock = lock;
      this.conditionObject = conditionObject;
    }

    public void run() {
      lock.lock();
      try {
        conditionObject.await();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } finally {
        lock.unlock();
      }
    }
  }

  private class TestRunnable2 implements Runnable {
    private ReentrantLock lock;

    public TestRunnable2(ReentrantLock lock) {
      this.lock = lock;
    }

    public void run() {
      lock.lock();
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } finally {
        lock.unlock();
        try {
          barrier2.await();
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
  }

  private class InterruptedRunnable implements Runnable {
    private ReentrantLock lock;

    public InterruptedRunnable(ReentrantLock lock) {
      this.lock = lock;
    }

    public void run() {
      try {
        try {
          lock.lock();
        } catch (TCRuntimeException e) {
          Assert.assertFalse(isCausedByInterruptedException(e));
        }
      } finally {
        lock.unlock();
      }

    }
  }

  private boolean isCausedByInterruptedException(TCRuntimeException e) {
    if (e.getCause() instanceof InterruptedException) {
      return true;
    } else {
      return false;
    }
  }

  private class TestTryLockFailRunnable implements Runnable {
    private String        lockId;
    private CyclicBarrier barrier;

    public TestTryLockFailRunnable(String lockId, CyclicBarrier barrier) {
      this.lockId = lockId;
      this.barrier = barrier;
    }

    public void run() {
      try {
        boolean locked = ManagerUtil.tryBeginLock(lockId, LockLevel.WRITE);

        Assert.assertFalse(locked);
        this.barrier.await();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  /*
   * private static class TestRunnable3 implements Runnable { private ReentrantLock lock; private Object sharedObject;
   * public TestRunnable3(ReentrantLock lock, Object obj) { this.lock = lock; this.sharedObject = obj; } public void
   * run() { synchronized(sharedObject) { try { lock.lock(); sharedObject.wait(); } catch (InterruptedException e) {
   * lock.unlock(); } finally { lock.unlock(); } } } }
   */

  private static class DataRoot {
    private ReentrantLock unfairLock      = new ReentrantLock();

    // When an reentrant lock is shared,
    // the fairness is current not supported.
    private ReentrantLock fairLock        = new ReentrantLock(true);
    private Condition     unfairCondition = unfairLock.newCondition();
    private Condition     fairCondition   = fairLock.newCondition();
    private ReentrantLock lazyLock;
    private int           data;

    public DataRoot() {
      super();
    }

    public ReentrantLock getFairLock() {
      return fairLock;
    }

    public void setFairLock(ReentrantLock fairLock) {
      this.fairLock = fairLock;
    }

    public ReentrantLock getUnfairLock() {
      return unfairLock;
    }

    public void setUnfairLock(ReentrantLock unfairLock) {
      this.unfairLock = unfairLock;
    }

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    public ReentrantLock getLazyLock() {
      return lazyLock;
    }

    public void setLazyLock(ReentrantLock lazyLock) {
      this.lazyLock = lazyLock;
    }

    public Condition getUnfairCondition() {
      return unfairCondition;
    }

    public void setUnfairCondition(Condition unfairCondition) {
      this.unfairCondition = unfairCondition;
    }

    public Condition getFairCondition() {
      return fairCondition;
    }

    public void setFairCondition(Condition fairCondition) {
      this.fairCondition = fairCondition;
    }

    public void clear() {
      this.data = 0;
    }
  }

  private static class WorkItem {
    static final WorkItem STOP = new WorkItem("STOP");

    private final String  name;

    WorkItem(String name) {
      this.name = name;
    }

    boolean isStop() {
      return STOP.name.equals(name);
    }

    public String toString() {
      return this.name;
    }
  }
}
