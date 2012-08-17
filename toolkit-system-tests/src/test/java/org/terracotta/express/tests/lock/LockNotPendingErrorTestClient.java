/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import junit.framework.Assert;

public class LockNotPendingErrorTestClient extends ClientBase {

  private ToolkitBarrier nodeBarrier;
  private ToolkitBarrier appThreadBarrier;
  private Toolkit        toolkit;

  public LockNotPendingErrorTestClient(String[] args) {
    super(args);
  }

  public static void debug(String msg) {
    System.out.println("###### " + System.currentTimeMillis() + " " + Thread.currentThread().getName() + ": " + msg);
  }

  @Override
  protected void test(Toolkit toolkit2) throws Throwable {
    this.toolkit = toolkit2;
    nodeBarrier = getBarrierForAllClients();
    appThreadBarrier = toolkit.getBarrier("appThreadBarrier", 2);

    final int index = nodeBarrier.await();
    testOnSingleNode(index);
    nodeBarrier.await();
    testOnMultipleNodes(index);
  }

  private void testOnSingleNode(final int index) throws Exception {
    if (index == 0) {
      System.out.println("Testing on Single node");
      final Map<String, Throwable> exceptions = Collections.synchronizedMap(new HashMap<String, Throwable>());
      ToolkitReadWriteLock readWriteLock = toolkit.getReadWriteLock("test-lock-id");
      final Lock namedReadLock = readWriteLock.readLock();
      final Lock namedWriteLock = readWriteLock.writeLock();

      Thread readLockThread = getReadLockThread(exceptions, namedReadLock);
      Thread tryWriteLockThread = getWriteLockThread(exceptions, namedWriteLock);

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
    ToolkitReadWriteLock readWriteLock = toolkit.getReadWriteLock("test-lock-id");
    final Lock namedReadLock = readWriteLock.readLock();
    final Lock namedWriteLock = readWriteLock.writeLock();

    Thread readLockThread = getReadLockThread(exceptions, namedReadLock);
    Thread tryWriteLockThread = getWriteLockThread(exceptions, namedWriteLock);

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
            boolean acquired = namedLock.tryLock();
            if (acquired) {
              try {
                acquiredCount++;
              } finally {
                namedLock.unlock();
              }
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
            namedLock.lock();
            try {
              acquiredCount++;
            } finally {
              namedLock.unlock();
            }
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
}
