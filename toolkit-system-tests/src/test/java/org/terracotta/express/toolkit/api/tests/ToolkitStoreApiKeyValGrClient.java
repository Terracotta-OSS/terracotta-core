/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.store.ToolkitStore;

import java.util.HashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class ToolkitStoreApiKeyValGrClient extends AbstractToolkitApiTestClientUtil {
  private ToolkitStore store;
  private Toolkit      toolkit;

  public ToolkitStoreApiKeyValGrClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setDs(toolkit);
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    super.test(toolkit);
    this.test();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    super.test(toolkit);
    this.test();
  }

  protected void test() throws InterruptedException, BrokenBarrierException {
    checkDestroy();
    checkGetName();
    checkIsDestroyed();
    checkGetAll();
    checkCreateLockForKey();
    checkRemoveNoReturn();
    checkPutNoReturn();
  }

  private void checkCreateLockForKey() throws InterruptedException, BrokenBarrierException {
    setUp();

    try {
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      index = barrier.await();
      barrier.await();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      Thread thread1 = new Thread(new Runnable() {
        @Override
        public void run() {
          try {

            System.out.println("Thread1 Started");
            System.out.println("Thread1 Asking for lock");
            cyclicBarrier.await();
            ToolkitReadWriteLock rwLock = store.createLockForKey(10);
            System.out.println("Lock Acquired by thread1");
            try {
              cyclicBarrier.await();
              Thread.sleep(50000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              rwLock.writeLock().unlock();
            }
          } catch (Throwable t) {
            Assert.fail();

          }
        }
      });
      Thread thread2 = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            System.err.println("Starting Thread 2");
            cyclicBarrier.await();
            cyclicBarrier.await();
            long now = System.currentTimeMillis();
            System.err.println("THread2 Attempting to get ");
            store.get(10);
            System.err.println("THread2 got the value");
            long diff = System.currentTimeMillis() - now;
            Assert.assertTrue("CreateLock not working Properly " + diff + "MilliSecs", diff >= 40000);

          } catch (Throwable t) {
            Assert.fail();
          }
        }
      });
      index = barrier.await();
      if (index == 0) {
        System.err.println("Starting Threads");
        thread1.start();
        thread2.start();
        System.err.println("Started Threads");
        System.err.println("joining Threads");
        thread1.join();
        thread2.join();
        System.err.println("Joined Threads");
      }
    } finally {
      tearDown();
    }
  }

  private void checkRemoveNoReturn() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        store.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertTrue(store.containsKey(keyValueGenerator.getKey(1)));
      index = barrier.await();
      if (index == 0) {
        store.removeNoReturn(keyValueGenerator.getKey(1));
      }
      barrier.await();
      Assert.assertFalse(store.containsKey(keyValueGenerator.getKey(1)));

    } finally {
      tearDown();
    }
  }

  private void checkPutNoReturn() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      this.index = barrier.await();
      if (index == 0) {
        store.putNoReturn(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertEquals(keyValueGenerator.getValue(1), store.get(keyValueGenerator.getKey(1)));

      this.index = barrier.await();
      if (index == 0) {
        store.putNoReturn(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }
      barrier.await();
      Assert.assertEquals(keyValueGenerator.getValue(2), store.get(keyValueGenerator.getKey(1)));
      Assert.assertFalse(keyValueGenerator.getValue(1).equals(store.get(keyValueGenerator.getKey(1))));
    } finally {
      tearDown();
    }
  }

  private HashSet getKeySet(int start, int count) {
    HashSet tmpHashSet = new HashSet();
    for (int i = start; i < start + count; i++) {
      tmpHashSet.add(i);
    }
    return tmpHashSet;
  }

  private void checkGetAll() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      index = barrier.await();
      if (index == 0) {
        HashSet keys = getKeySet(START, END);
        tempMap = store.getAll(keys);
        Assert.assertTrue(checkKeyValuePairs(START, END));
      }
      barrier.await();
    } finally {
      tearDown();
    }

  }

  @Override
  public void setDs(Toolkit toolkit) {
    barrier = toolkit.getBarrier("mybarr", 2);
    map = store = toolkit.getStore("myStore", null);
  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertEquals("myStore", store.getName());
    } finally {
      tearDown();
    }
  }

  @Override
  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        ToolkitStore toolkitStore = toolkit.getStore("tempStore", null);
        Assert.assertFalse(toolkitStore.isDestroyed());
        toolkitStore.destroy();
        Assert.assertTrue(toolkitStore.isDestroyed());
      }
      barrier.await();
    } finally {
      tearDown();
    }
  }

  @Override
  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      new Exception("" + index).printStackTrace();
      if (index == 0) {
        ToolkitStore toolkitStore = toolkit.getStore("tempStore", null);
        Assert.assertFalse(toolkitStore.isDestroyed());
        toolkitStore.destroy();
        Assert.assertTrue(toolkitStore.isDestroyed());
        new Exception("" + index).printStackTrace();
      }
      barrier.await();
    } finally {
      tearDown();
    }
  }
}
