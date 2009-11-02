/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class LockNotPendingErrorTestApp extends AbstractErrorCatchingTransparentApp {

  private CyclicBarrier nodeBarrier;
  private CyclicBarrier appThreadBarrier;

  public LockNotPendingErrorTestApp(final String appId, final ApplicationConfig cfg,
                                    final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LockNotPendingErrorTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("nodeBarrier", "nodeBarrier");
    spec.addRoot("appThreadBarrier", "appThreadBarrier");
    new CyclicBarrierSpec().visit(visitor, config);
  }

  public static void debug(String msg) {
    System.out.println("###### " + System.currentTimeMillis() + " " + Thread.currentThread().getName() + ": " + msg);
  }

  @Override
  protected void runTest() throws Throwable {
    nodeBarrier = new CyclicBarrier(getParticipantCount());
    appThreadBarrier = new CyclicBarrier(2);
    final int index = nodeBarrier.await();
    testOnSingleNode(index);
    nodeBarrier.await();
    testOnMultipleNodes(index);
  }

  private void testOnSingleNode(final int index) throws Exception {
    if (index == 0) {
      System.out.println("Testing on Single node");
      final Map<String, Throwable> exceptions = Collections.synchronizedMap(new HashMap<String, Throwable>());
      final Lock namedLock = new Lock("test-lock-id");

      Thread readLockThread = getReadLockThread(exceptions, namedLock);
      Thread tryWriteLockThread = getWriteLockThread(exceptions, namedLock);

      readLockThread.start();
      tryWriteLockThread.start();

      // wait for the threads to end...
      readLockThread.join();
      tryWriteLockThread.join();
      for (String thread : exceptions.keySet()) {
        Assert.fail("Thread \"" + thread + "\" failed with the exception: " + exceptions.get(thread));
        exceptions.get(thread).printStackTrace();
        break;
      }
      System.out.println("END Single node");
    }
  }

  private void testOnMultipleNodes(final int index) throws Exception {
    System.out.println("Testing on Multiple nodes");
    final Map<String, Throwable> exceptions = Collections.synchronizedMap(new HashMap<String, Throwable>());
    final Lock namedLock = new Lock("test-lock-id");

    Thread readLockThread = getReadLockThread(exceptions, namedLock);
    Thread tryWriteLockThread = getWriteLockThread(exceptions, namedLock);

    if (index == 0) readLockThread.start();
    else tryWriteLockThread.start();

    // wait for the threads to end...
    if (index == 0) readLockThread.join();
    else tryWriteLockThread.join();
    for (String thread : exceptions.keySet()) {
      Assert.fail("Thread \"" + thread + "\" failed with the exception: " + exceptions.get(thread));
      exceptions.get(thread).printStackTrace();
      break;
    }
    System.out.println("END Testing on Multiple nodes");
  }

  private Thread getWriteLockThread(final Map<String, Throwable> exceptions, final Lock namedLock) {
    Thread tryWriteLockThread = new Thread(new Runnable() {

      public void run() {
        try {
          int notAcquired = 0;
          int acquiredCount = 0;
          for (int i = 0; i < 5000; i++) {
            appThreadBarrier.await();
            if (exceptions.size() > 0) break;
            boolean acquired = namedLock.tryWriteLock();
            if (acquired) {
              acquiredCount++;
              namedLock.commitWriteLock();
            } else {
              notAcquired++;
            }
            appThreadBarrier.await();
          }
          debug("Acquired Count=" + acquiredCount + " Not acquired: " + notAcquired);
        } catch (Throwable e) {
          exceptions.put(Thread.currentThread().getName(), e);
        }

      }

    }, "TryWriteLockThread");
    return tryWriteLockThread;
  }

  private Thread getReadLockThread(final Map<String, Throwable> exceptions, final Lock namedLock) {
    Thread readLockThread = new Thread(new Runnable() {

      public void run() {
        try {
          int acquiredCount = 0;
          for (int i = 0; i < 5000; i++) {
            appThreadBarrier.await();
            if (exceptions.size() > 0) break;
            namedLock.getReadLock();
            acquiredCount++;
            namedLock.commitReadLock();
            appThreadBarrier.await();
          }
          debug("Acquired Count=" + acquiredCount);
        } catch (Throwable e) {
          exceptions.put(Thread.currentThread().getName(), e);
        }

      }

    }, "ReadLockThread");
    return readLockThread;
  }

  private static class Lock {
    private final String lockId;
    private final int    lockType;

    // for non-synchronous-write tests
    public Lock(final String lockId) {
      this(lockId, Manager.LOCK_TYPE_WRITE);
    }

    public Lock(final String lockId, final int lockType) {
      if (lockType != Manager.LOCK_TYPE_SYNCHRONOUS_WRITE && lockType != Manager.LOCK_TYPE_WRITE) { throw new AssertionError(
                                                                                                                             "Trying to set lockType to "
                                                                                                                                 + lockType
                                                                                                                                 + " -- must be either write or synchronous-write"); }

      this.lockType = lockType;
      this.lockId = lockId;
    }

    public void commitWriteLock() {
      ManagerUtil.commitLock(lockId, lockType);      
    }
    
    public void commitReadLock() {
      ManagerUtil.commitLock(lockId, Manager.LOCK_TYPE_READ);
    }
    
    public boolean tryWriteLock() {
      return ManagerUtil.tryBeginLock(lockId, lockType);
    }

    public void getReadLock() {
      ManagerUtil.beginLock(lockId, Manager.LOCK_TYPE_READ);
    }

  }
}