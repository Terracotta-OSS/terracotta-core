/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCObjectNotSharableException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
  private final Exit          exit               = new Exit();

  private final Random        random;

  private final ReentrantLock unsharedUnfairLock = new ReentrantLock();
  private final ReentrantLock unsharedFairLock   = new ReentrantLock(true);

  private final Object        testLockObject     = new Object();

  private final int           numOfPutters       = 1;
  private final int           numOfGetters;
  private final boolean       isCrashTest;

  public ReentrantLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());
    numOfGetters = getParticipantCount() - numOfPutters;
    isCrashTest = "true".equals(cfg.getAttribute(CRASH_TEST));
  }

  // TODO: We need to add a test case for a situation where an unshared ReentrantLock become
  // shared.
  public void run() {
    try {
      barrier.await();
      System.err.println("Testing toString on shared lock - A");
      toStringTest(root.getFairLock(), 0, 1);
      System.err.println("Testing toString on shared lock - B");
      toStringTest(root.getFairLock(), 1, 0);
      System.err.println("Testing toString on unshared lock");
      toStringTest(new ReentrantLock(), 0, 1);

      sharedUnSharedTesting();
      basicUnsharedSignalTesting();
      multipleReentrantLocksTesting();
      singleNodeTryBeginLockTesting();
      variousLockUnLockPatternTesting();

      System.err.println("Testing unfair lock ...");

      basicSignalTesting(root.getUnfairLock(), root.getUnfairCondition());
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

      tryLockTest(root.getUnfairLock());

      if (!lockInterruptiblyTest(root.getUnfairLock())) return;

      System.err.println("Testing fair lock ...");

      basicSignalTesting(root.getFairLock(), root.getFairCondition());
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

      tryLockTest(root.getFairLock());

      if (!lockInterruptiblyTest(root.getFairLock())) return;

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void toStringTest(ReentrantLock lock, int lockedByNode, int watcherNode) throws Exception {
    Assert.assertTrue(lockedByNode == 0 || lockedByNode == 1);
    Assert.assertTrue(watcherNode == 0 || watcherNode == 1);
    int index = barrier.await();
    if (index == lockedByNode) {
      System.err.println(index + " Lock just before locking: " + lock.toString());
      barrier2.await();
      lock.lock();
      System.err.println(index + " Locked");
      barrier2.await();
      try {
        System.err.println(index + " Lock after locking: " + lock.toString());
      } finally {
        System.err.println(index + " Unlocking lock...");
        barrier2.await();
        lock.unlock();
        System.err.println(index + " Unlocked");
        barrier2.await();
        System.err.println(index + " Lock after unlock: " + lock.toString());
      }
    } else if (index == watcherNode) {
      System.err.println(index + " Lock just before locking: " + lock.toString());
      barrier2.await();
      barrier2.await();
      System.err.println(index + " Lock after locking (by other node/thread): " + lock.toString());
      barrier2.await();
      barrier2.await();
      System.err.println(index + " Lock after unlock: " + lock.toString());
    } else {
      System.err.println(index + " Not participating in toStringTest :(");
    }
    barrier.await();
  }

  private void tryLockTest(final ReentrantLock lock) throws Exception {
    int index = barrier.await();

    if (index == 0) {
      System.err.println("Locking in client 1");
      lock.lock();
      try {
        barrier2.await();
        barrier2.await();
      } finally {
        System.err.println("Unlocking in client 1");
        lock.unlock();
      }

    } else if (index == 1) {
      barrier2.await();

      System.err.println("Testing try lock failures in client 2");
      int countTryLockSucceeded = 0;
      int countTryLockFailed = 0;
      int countLocked = 0;
      int countUnLocked = 0;
      for (int i = 0; i < 100; i++) {
        if (!lock.tryLock()) {
          countTryLockFailed++;
          if (lock.isLocked()) {
            countLocked++;
          } else {
            countUnLocked++;
          }
        } else {
          countTryLockSucceeded++;
        }
      }
      System.out.println("tryLock succeeded: " + countTryLockSucceeded);
      System.out.println("tryLock failed: " + countTryLockFailed);
      System.out.println("unlocked: " + countUnLocked);
      System.out.println("locked: " + countLocked);
      Assert.assertEquals(100, countLocked);
      barrier2.await();

      System.err.println("Locking in client 2");
      lock.lock();
      System.err.println("Unlocking in client 2");
      lock.unlock();

      System.err.println("Testing try lock successes in client 2");
      int count = 0;
      for (int i = 0; i < 100; i++) {
        if (!lock.tryLock()) {
          count++;
        }
      }
      Assert.assertEquals(0, count);
    }

    barrier.await();
  }

  private void sharedUnSharedTesting() throws Exception {
    int index = barrier.await();

    if (index == 0) {
      ReentrantLock lock = new ReentrantLock();
      lock.lock();
      synchronized (root) {
        try {
          root.setLazyLock(lock);
          throw new AssertionError("Should have thrown a TCObjectNotSharableException.");
        } catch (TCObjectNotSharableException e) {
          // expected
        }
      }
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

      ManagerUtil.beginLock(lockId, Manager.LOCK_TYPE_WRITE);
      thread1.start();
      localBarrier.await();
      ManagerUtil.commitLock(lockId, Manager.LOCK_TYPE_WRITE);
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

  private void basicUnsharedSignalTesting() throws Exception {
    if (barrier.await() == 0) {
      final ReentrantLock lock = new ReentrantLock();
      final Condition condition = lock.newCondition();

      Thread[] threads = new Thread[10];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new Runnable() {
          public void run() {
            lock.lock();
            try {
              condition.awaitUninterruptibly();
              System.out.println("Thread Waking");
            } finally {
              lock.unlock();
            }
          }
        });
      }

      for (Thread t : threads) {
        t.start();
      }

      while (lock.getWaitQueueLength(condition) < threads.length) {
        Thread.sleep(100);
      }

      int signalCount = 0;
      while (lock.hasWaiters(condition)) {
        lock.lock();
        try {
          System.out.println("Signalling...");
          condition.signal();
        } finally {
          lock.unlock();
        }
        signalCount++;
        Thread.sleep(100);
      }

      int expected = threads.length;
      Assert.assertEquals("Signal calls needed to wake " + expected + " threads", expected, signalCount);

      for (Thread t : threads) {
        t.join(1000);
        Assert.assertFalse(t.isAlive());
      }
    }
  }

  private void basicSignalTesting(final ReentrantLock lock, final Condition condition) throws Exception {
    int index = barrier.await();

    final AtomicInteger count = new AtomicInteger();
    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(new Runnable() {
        public void run() {
          lock.lock();
          try {
            count.incrementAndGet();
            condition.awaitUninterruptibly();
            System.out.println("\tClient " + ManagerUtil.getClientID() + " Thread Waking");
          } finally {
            lock.unlock();
          }
        }
      });
    }

    for (Thread t : threads) {
      t.start();
    }

    barrier.await();

    // lock.getWaitQueueLength(condition) is busted for greedy locks - I don't trust it.
    while (count.get() < threads.length) {
      Thread.sleep(100);
    }

    barrier.await();

    if (index == 0) {
      int signalCount = 0;
      while (lock.hasWaiters(condition) || signalCount == 0) {
        lock.lock();
        try {
          System.out.println("Signalling...");
          condition.signal();
        } finally {
          lock.unlock();
        }
        signalCount++;
        Thread.sleep(100);
      }

      int threadCount = getParticipantCount() * threads.length;
      int expected = threadCount / 2;
      Assert.assertTrue(signalCount + " signal calls needed to wake " + threadCount + " threads, expected at least "
                        + expected, signalCount > expected);
    }

    barrier.await();

    for (Thread t : threads) {
      t.join(1000);
      Assert.assertFalse(t.isAlive());
    }
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
      synchronized (thread) {
        thread.interrupt();
        lock.unlock();
        if (thread.isAlive()) {
          thread.wait();
        }
      }
    }
    barrier.await();
  }

  private boolean lockInterruptiblyTest(final ReentrantLock lock) throws Exception {
    // Cleanup the test lock
    while (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }

    if (barrier.await() == 0) {
      System.out.println("Testing ReentrantLock.lockInterruptibly()...");
      System.out
          .println("InterruptedExceptions may be logged.  Without an associated AssertionError they are harmless.");

    }

    for (int i = 0; i < 10; i++) {
      doLockInterruptiblyTestCycle(lock, i);
      if (exit.shouldExit()) return false;
    }

    return true;
  }

  private void doLockInterruptiblyTestCycle(final ReentrantLock lock, int cycle) throws Exception {
    int index = barrier.await();

    try {
      if (index == 0) {
        lock.lockInterruptibly();
        System.out.print("[" + ManagerUtil.getClientID() + "L");
        System.out.flush();
      }
    } finally {
      barrier.await();
    }
    if (exit.shouldExit()) return;

    try {
      if (index != 0) {
        for (int i = 0; i < 10; i++) {
          final int sleep = 1 << i;
          Thread t = new Thread() {
            @Override
            public void run() {
              try {
                System.out.print(":LK" + ManagerUtil.getClientID());
                lock.lockInterruptibly();
              } catch (InterruptedException e) {
                System.out.print(":LI" + ManagerUtil.getClientID());
              }
            }
          };
          t.start();

          try {
            Thread.sleep(sleep);
          } catch (InterruptedException e) {
            //
          }
          t.interrupt();
          System.out.print(":INT" + ManagerUtil.getClientID());
          /*
           * In crash tests if the L2 crashes at the wrong moment then this thread join will fail unless we factor in
           * the time for L2 restart in the timeout.
           */
          t.join(5 * 60 * 1000);

          if (t.isAlive()) {
            synchronized (System.err) {
              System.err.println("Stack Trace Of Interruptibly Locking Thread [Client " + ManagerUtil.getClientID()
                                 + "]");
              for (StackTraceElement e : t.getStackTrace()) {
                System.err.println("\tat " + e);
              }
            }
            t.stop();
            exit.toggle();
            throw new AssertionError("Thread " + t + " failed to respond to an interrupt during lockInterruptibly()");
          }
        }
      }
    } finally {
      barrier.await();
    }
    if (exit.shouldExit()) return;

    if (index == 0) {
      lock.unlock();
      System.out.println(":U]");
    }
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

    lock.lock();
    try {
      Assert.assertEquals(10, root.getData());
    } finally {
      lock.unlock();
    }

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
            if (isLocked) {
              lock.unlock();
            }
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
            if (isLocked) {
              lock.unlock();
            }
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
      if (isLocked) {
        lock.unlock();
      }

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
      if (isLocked) {
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
            if (isLocked) {
              lock.unlock();
            }
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
      if (isLocked) {
        lock.unlock();
      }

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
            if (isLocked) {
              lock.unlock();
            }
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
            if (isLocked) {
              nonSharedLock.unlock();
            }
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
            if (isLocked) {
              nonSharedLock.unlock();
            }
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
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("barrier3", "barrier3");
    spec.addRoot("exit", "exit");
    spec.addRoot("queue", "queue");
    spec.addRoot("testLockObject", "testLockObject");
  }

  private static class TestRunnable1 implements Runnable {
    private final ReentrantLock lock;
    private final Condition     conditionObject;

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

  private class InterruptedRunnable implements Runnable {
    private final ReentrantLock lock;

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
        synchronized (this) {
          this.notifyAll();
        }
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

  private static class TestTryLockFailRunnable implements Runnable {
    private final String        lockId;
    private final CyclicBarrier barrier;

    public TestTryLockFailRunnable(String lockId, CyclicBarrier barrier) {
      this.lockId = lockId;
      this.barrier = barrier;
    }

    public void run() {
      try {
        boolean locked = ManagerUtil.tryBeginLock(lockId, Manager.LOCK_TYPE_WRITE);

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
    private final ReentrantLock unfairLock      = new ReentrantLock();

    // When an reentrant lock is shared,
    // the fairness is current not supported.
    private final ReentrantLock fairLock        = new ReentrantLock(true);
    private final Condition     unfairCondition = unfairLock.newCondition();
    private final Condition     fairCondition   = fairLock.newCondition();
    @SuppressWarnings("unused")
    private ReentrantLock       lazyLock;
    private int                 data;

    public DataRoot() {
      super();
    }

    public ReentrantLock getFairLock() {
      return fairLock;
    }

    public ReentrantLock getUnfairLock() {
      return unfairLock;
    }

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    public void setLazyLock(ReentrantLock lazyLock) {
      this.lazyLock = lazyLock;
    }

    public Condition getUnfairCondition() {
      return unfairCondition;
    }

    public Condition getFairCondition() {
      return fairCondition;
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

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static class Exit {
    private boolean exit = false;

    synchronized boolean shouldExit() {
      return exit;
    }

    synchronized void toggle() {
      exit = true;
    }
  }
}
