/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.junit.Assert;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractToolkitApiTestClientUtil extends AbstractMapApiTestClientUtil {
  protected ConcurrentMap chm;
  protected Map           tempMap;

  public AbstractToolkitApiTestClientUtil(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    super.test(toolKit);
    this.test();
  }

  protected void test() throws Throwable {
    super.map = chm;
    super.test(this.toolkit);
    checkPutIfAbsent();
    checkRemoveTwoArgs();
    checkReplaceTwoARgs();
    checkReplaceThreeArgs();
  }

  private void checkReplaceTwoARgs() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        chm.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals(chm.get(keyValueGenerator.getKey(1)), keyValueGenerator.getValue(1));
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        chm.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }

      waitForAllClientsToReachHere();
      Assert.assertFalse(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertTrue(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));

    } finally {
      clearDs();
    }
  }

  private void checkReplaceThreeArgs() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      clientIndex = barrier.await();
      System.err.println("****In Method checkReplaceThreeArgs****");
      System.err.println("******clientIndex = " + clientIndex + "******");
      if (clientIndex == 0) {
        chm.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals(chm.get(keyValueGenerator.getKey(1)), keyValueGenerator.getValue(1));
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        chm.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2), keyValueGenerator.getValue(3));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertFalse(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        chm.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1), keyValueGenerator.getValue(2));
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertTrue(chm.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));
      System.err.println("******Exiting checkReplaceThreeArgs*****");
    } finally {
      clearDs();
    }
  }

  private void checkRemoveTwoArgs() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      clientIndex = barrier.await();
      if (clientIndex == 1) {
        chm.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(chm.containsKey(keyValueGenerator.getKey(1)));
      clientIndex = barrier.await();
      if (clientIndex == 1) {
        chm.remove(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(chm.containsKey(keyValueGenerator.getKey(1)));
      clientIndex = barrier.await();
      if (clientIndex == 1) {
        chm.remove(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(chm.containsKey(keyValueGenerator.getKey(1)));
    } finally {
      clearDs();
    }
  }

  private void checkPutIfAbsent() throws Exception {
    waitForAllClientsToReachHere();
    try {
      Assert.assertFalse(chm.containsKey(keyValueGenerator.getKey(1)));
      clientIndex = barrier.await();
      System.err.println("****In Method checkPutIfAbsent****");
      System.err.println("******clientIndex = " + clientIndex + "******");
      if (clientIndex == 1) {
        chm.putIfAbsent(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
        System.err.println("Key is " + keyValueGenerator.getKey(1) + " Value is " + keyValueGenerator.getValue(1));
        System.err.println(chm.get(keyValueGenerator.getKey(1)));
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            return chm.get(keyValueGenerator.getKey(1)) != null;
          }
        });
        System.err.println("just after put " + chm.get(keyValueGenerator.getKey(1)));
        System.err.println("Put done");
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals("clientIndex = " + clientIndex, keyValueGenerator.getValue(1),
                          chm.get(keyValueGenerator.getKey(1)));
      Assert.assertTrue(chm.containsKey(keyValueGenerator.getKey(1)));
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        chm.putIfAbsent(keyValueGenerator.getKey(1), keyValueGenerator.getKey(2));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(chm.containsKey(keyValueGenerator.getKey(1)));
      Assert.assertNotSame(keyValueGenerator.getValue(2), chm.get(keyValueGenerator.getKey(1)));

    } finally {
      clearDs();
    }
  }

  void doSomePutsInTempMap(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      tempMap.put(keyValueGenerator.getKey(iterator), keyValueGenerator.getValue(iterator));
    }
  }

}
