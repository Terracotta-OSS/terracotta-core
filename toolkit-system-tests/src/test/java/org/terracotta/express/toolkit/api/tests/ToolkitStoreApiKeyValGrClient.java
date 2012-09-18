/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class ToolkitStoreApiKeyValGrClient extends AbstractToolkitApiTestClientUtil {
  protected ToolkitStore store;
  protected Toolkit    toolkit;

  public ToolkitStoreApiKeyValGrClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    testWithStrongStore(toolKit);
    index = barrier.await();
    if (index == 0) {
      clearDs();
    }
    barrier.await();
    testWithEventualStore(toolKit);
  }

  private void clearDs() {
    System.err.println("^^^^^^^^^DESTROYING STORE^^^^^^^^^^^^");
    store.destroy();
    System.err.println("Destroyed");
  }

  private void testWithStrongStore(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setStrongDs(toolkit, NAME_OF_DS);

    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    this.test();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    this.test();
  }

  private void testWithEventualStore(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setEventualDs(toolkit, NAME_OF_DS);
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    this.test();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    this.test();
  }

  @Override
  protected void test() throws Exception {
    super.test();
    checkDestroy();
    checkGetName();
    checkIsDestroyed();
    checkGetAll();
    checkRemoveNoReturn();
    checkPutNoReturn();
    checkCreateLockForKey();
  }

  private void checkCreateLockForKey() throws InterruptedException, BrokenBarrierException {
    // run this test for only Store with strong consistency
    if (store.getConfiguration().getString(ToolkitStoreConfigFields.CONSISTENCY_FIELD_NAME)
        .equals(ToolkitStoreConfigFields.Consistency.EVENTUAL.toString())) {
      System.err
          .println("*************************Need not checkCreateLockForKey for Eventual Store************************");
 return;
    }
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
            rwLock.writeLock().lock();
            try {
              cyclicBarrier.await();
              cyclicBarrier.await();
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              rwLock.writeLock().unlock();
              System.err.println("Lock relaesed by thread1");
            }
          } catch (Throwable t) {
            System.err.println("Exception in thread1,Stack Trace :");
            t.printStackTrace();
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
            cyclicBarrier.await();
            System.err.println("THread2 Attempting to get ");
            store.get(10);
            System.err.println("THread2 got the value");
            long diff = System.currentTimeMillis() - now;
            System.err.println("waited for : " + diff);
            Assert.assertTrue("CreateLock not working Properly " + diff + "MilliSecs",
 diff >= 4000);

          } catch (Throwable t) {
            System.err.println("Exception in thread2,Stack Trace :");
            t.printStackTrace();
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

  protected Set getKeySet(int start, int count) {
    Set tmpHashSet = new TreeSet();
    for (int i = start; i < start + count; i++) {
      tmpHashSet.add(keyValueGenerator.getKey(i));
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
        Set keys = getKeySet(START, END);
        tempMap = store.getAll(keys);
        Assert.assertTrue(checkKeyValuePairs(START, END));
      }
      barrier.await();
    } finally {
      tearDown();
    }

  }


  @Override
  protected void setStrongDs(Toolkit toolkit, String name) {
    barrier = toolkit.getBarrier("mybarr", 2);
    ToolkitStoreConfigBuilder configBuilder = new ToolkitStoreConfigBuilder().consistency(Consistency.STRONG);
    Configuration config = configBuilder.build();
    map = store = toolkit.getStore(name, config, String.class);

  }

  @Override
  protected void setEventualDs(Toolkit toolkit, String name) {
    barrier = toolkit.getBarrier("mybarr", 2);
    ToolkitStoreConfigBuilder configBuilder = new ToolkitStoreConfigBuilder().consistency(Consistency.EVENTUAL);
    Configuration config = configBuilder.build();
    map = store = toolkit.getStore(name, config, String.class);

  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertEquals(NAME_OF_DS, store.getName());
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
