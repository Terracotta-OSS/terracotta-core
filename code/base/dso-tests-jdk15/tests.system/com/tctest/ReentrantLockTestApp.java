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
import com.tc.object.config.TransparencyClassSpec;
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
  private static final int    NUM_OF_PUTS        = 1000;

  private final DataRoot      root               = new DataRoot();
  private final List          queue              = new LinkedList();
  private final CyclicBarrier barrier2           = new CyclicBarrier(2);
  private final CyclicBarrier barrier;

  private final Random        random;

  private final ReentrantLock unsharedUnfairLock = new ReentrantLock();
  private final ReentrantLock unsharedFairLock   = new ReentrantLock(true);

  private final Object        testLockObject     = new Object();

  private int                 numOfPutters       = 1;
  private int                 numOfGetters;

  public ReentrantLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());
    numOfGetters = getParticipantCount() - numOfPutters;
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
      basicLockTesting(root.getUnfairLock());
      lockSyncLockTesting(root.getUnfairLock());

      threadInterruptedLockTesting(root.getUnfairLock());

      System.err.println("Testing fair lock ...");

      basicConditionVariableTesting(root.getFairLock(), root.getFairCondition());
      basicConditionVariableWaitTesting(root.getFairLock(), root.getFairCondition());
      basicUnsharedLockTesting(unsharedFairLock);
      basicLockTesting(root.getFairLock());
      lockSyncLockTesting(root.getFairLock());

      threadInterruptedLockTesting(root.getFairLock());

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void clear() throws Exception {
    synchronized (root) {
      if (root.getIndex() != 0) {
        root.setIndex(0);
      }
    }

    barrier.await();
  }

  private void sharedUnSharedTesting() throws Exception {

    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.await();

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

  }

  /**
   * This testcase is to ensure that the unlocking sequence can be different from the locking sequence. For example,
   * lock1.lock(), lock2.lock(), lock1.unlock(), and lock2.unlock() as well as lock1.lock(), lock2.lock(),
   * lock2.unlock(), lock1.unlock() will both work correctly.
   */
  private void multipleReentrantLocksTesting() throws Exception {
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      } else if (index == 1) {
        root.setIndex(2);
      } else if (index == 2) {
        root.setIndex(3);
      }
    }

    barrier.await();

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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      } else if (index == 1) {
        root.setIndex(2);
      } else if (index == 2) {
        root.setIndex(3);
      }
    }

    barrier.await();

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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      } else if (index == 1) {
        root.setIndex(2);
      } else if (index == 2) {
        root.setIndex(3);
      }
    }

    barrier.await();

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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.await();

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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.await();
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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      }
    }

    barrier.await();

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
    clear();
    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      }
    }
    barrier.await();
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
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      }
    }

    barrier.await();

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

  /**
   * This test case provides basic testing to the API of an reentrant lock when an reentrant lock is shared.
   */
  private void basicLockTesting(ReentrantLock lock) throws Exception {
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      if (index == 0) {
        root.setIndex(1);
      }
    }

    barrier.await();

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

    if (index == 0) {
      Assert.assertEquals(0, lock.getQueueLength());
      Assert.assertFalse(lock.isLocked());
    } else {
      Assert.assertTrue(lock.isLocked()); // due to greedy lock.
    }

    barrier.await();
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
    private int           index;

    public DataRoot() {
      this.index = 0;
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
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
      this.index = 0;
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
