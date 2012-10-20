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

  public ToolkitStoreApiKeyValGrClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    testWithStrongStore(toolKit);
    clientIndex = barrier.await();
    if (clientIndex == 0) {
      destroyDs();
    }
    waitForAllClientsToReachHere();
    testWithEventualStore(toolKit);
  }

  private void destroyDs() {
    System.err.println("^^^^^^^^^DESTROYING STORE^^^^^^^^^^^^");
    store.destroy();
    System.err.println("Destroyed");
  }

  private void testWithStrongStore(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setDs(toolkit, NAME_OF_DS, STRONG);

    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    this.test();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    this.test();
  }

  private void testWithEventualStore(Toolkit toolKit) throws Throwable {
    this.toolkit = toolKit;
    setDs(toolkit, NAME_OF_DS, EVENTUAL);
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    this.test();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    this.test();
  }

  @Override
  protected void test() throws Throwable {
    super.chm = store;
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
    String methodName = "checkCreateLockForKey";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " for clientIndex = " + clientIndex);
    try {
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      clientIndex = barrier.await();
      waitForAllClientsToReachHere();
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
            Assert.assertTrue("CreateLock not working Properly " + diff + "MilliSecs", diff >= 4000);

          } catch (Throwable t) {
            System.err.println("Exception in thread2,Stack Trace :");
            t.printStackTrace();
            Assert.fail();
          }
        }
      });
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        System.err.println("Starting Threads");
        thread1.start();
        thread2.start();
        System.err.println("Started Threads");
        System.err.println("joining Threads");
        thread1.join();
        thread2.join();
        System.err.println("Joined Threads");
      }
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void checkRemoveNoReturn() throws InterruptedException, BrokenBarrierException {
    try {
      String methodName = "checkRemoveNoReturn";
      clientIndex = waitForAllClientsToReachHere();
      log("Entering " + methodName + " for clientIndex = " + clientIndex);
      if (clientIndex == 0) {
        store.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(store.containsKey(keyValueGenerator.getKey(1)));
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        store.removeNoReturn(keyValueGenerator.getKey(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(store.containsKey(keyValueGenerator.getKey(1)));

      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void checkPutNoReturn() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkPutNoReturn";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        store.putNoReturn(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals(keyValueGenerator.getValue(1), store.get(keyValueGenerator.getKey(1)));

      this.clientIndex = barrier.await();
      if (clientIndex == 0) {
        store.putNoReturn(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals(keyValueGenerator.getValue(2), store.get(keyValueGenerator.getKey(1)));
      Assert.assertFalse(keyValueGenerator.getValue(1).equals(store.get(keyValueGenerator.getKey(1))));
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
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
    String methodName = "checkGetAll";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        Set keys = getKeySet(START_INDEX, END_INDEX);
        tempMap = store.getAll(keys);
        Assert.assertTrue(allKeyValuePairsArePresent(START_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  protected void setStrongDs(Toolkit toolkit, String name) {
    super.toolkit = toolkit;
    barrier = toolkit.getBarrier("mybarr", 2);
    ToolkitStoreConfigBuilder configBuilder = new ToolkitStoreConfigBuilder().consistency(Consistency.STRONG);
    Configuration config = configBuilder.build();
    chm = store = toolkit.getStore(name, config, String.class);

  }

  protected void setEventualDs(Toolkit toolkit, String name) {
    super.toolkit = toolkit;
    barrier = toolkit.getBarrier("mybarr", 2);
    ToolkitStoreConfigBuilder configBuilder = new ToolkitStoreConfigBuilder().consistency(Consistency.EVENTUAL);
    Configuration config = configBuilder.build();
    chm = store = toolkit.getStore(name, config, String.class);

  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkGetName";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      Assert.assertEquals(NAME_OF_DS, store.getName());
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  @Override
  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkIsDestroyed";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        ToolkitStore toolkitStore = toolkit.getStore("tempStore", null);
        Assert.assertFalse(toolkitStore.isDestroyed());
        toolkitStore.destroy();
        Assert.assertTrue(toolkitStore.isDestroyed());
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  @Override
  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkDestroy";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " for clientIndex = " + clientIndex);
    try {
      new Exception("" + clientIndex).printStackTrace();
      if (clientIndex == 0) {
        ToolkitStore toolkitStore = toolkit.getStore("tempStore", null);
        Assert.assertFalse(toolkitStore.isDestroyed());
        toolkitStore.destroy();
        Assert.assertTrue(toolkitStore.isDestroyed());
        new Exception("" + clientIndex).printStackTrace();
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  @Override
  public void setDs(Toolkit toolkit, String name, String strongOrEventual) {
    if (strongOrEventual.equals(STRONG)) {
      setStrongDs(toolkit, name);
    } else {
      setEventualDs(toolkit, name);
    }
  }
}
