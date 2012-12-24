/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.junit.Assert;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.TCInt;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.test.config.model.TestConfig;

public class SecondryTypesDestoryTest extends AbstractToolkitTestBase {
  public SecondryTypesDestoryTest(TestConfig testConfig) {
    super(testConfig, SecondryTypesDestoryTestClient.class);
  }

  public static class SecondryTypesDestoryTestClient extends ClientBase {

    public SecondryTypesDestoryTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {

      testSet(toolkit);
      testSortedSet(toolkit);
      testBlockingQueue(toolkit);
      testAtomicLong(toolkit);
      testBarrier(toolkit);

    }

    private void testBarrier(Toolkit toolkit) {
      ToolkitBarrier barrier1 = toolkit.getBarrier("TestBarrier", 2);
      ToolkitBarrier barrier2 = toolkit.getBarrier("TestBarrier", 2);
      assertEqual(barrier1, barrier2, "Barrier");
      barrier1.destroy();
      ToolkitBarrier barrier3 = toolkit.getBarrier("TestBarrier", 2);
      assertNotEqual(barrier1, barrier3, "Barrier");
    }

    private void testAtomicLong(Toolkit toolkit) {
      ToolkitAtomicLong atomicLong1 = toolkit.getAtomicLong("TestAtomicLong");
      ToolkitAtomicLong atomicLong2 = toolkit.getAtomicLong("TestAtomicLong");
      assertEqual(atomicLong1, atomicLong2, "AtomicLong");
      atomicLong1.destroy();
      ToolkitAtomicLong atomicLong3 = toolkit.getAtomicLong("TestAtomicLong");
      assertNotEqual(atomicLong1, atomicLong3, "AtomicLong");
    }

    private void testBlockingQueue(Toolkit toolkit) {
      ToolkitBlockingQueue blockingQueue1 = toolkit.getBlockingQueue("TestBlockingQueue", null);
      ToolkitBlockingQueue blockingQueue2 = toolkit.getBlockingQueue("TestBlockingQueue", null);
      assertEqual(blockingQueue1, blockingQueue2, "BlockingQueue");
      blockingQueue1.destroy();
      ToolkitBlockingQueue blockingQueue3 = toolkit.getBlockingQueue("TestBlockingQueue", null);
      assertNotEqual(blockingQueue1, blockingQueue3, "BlockingQueue");
    }

    private void testSortedSet(Toolkit toolkit) {
      ToolkitSortedSet<TCInt> sortedSet1 = toolkit.getSortedSet("TestSortedSet", TCInt.class);
      ToolkitSortedSet<TCInt> sortedSet2 = toolkit.getSortedSet("TestSortedSet", TCInt.class);
      assertEqual(sortedSet1, sortedSet2, "SortedSet");
      sortedSet1.destroy();
      ToolkitSortedSet<TCInt> sortedSet3 = toolkit.getSortedSet("TestSortedSet", TCInt.class);
      assertNotEqual(sortedSet1, sortedSet3, "SortedSet");
    }

    private void testSet(Toolkit toolkit) {
      ToolkitSet set1 = toolkit.getSet("TestSet", null);
      ToolkitSet set2 = toolkit.getSet("TestSet", null);
      assertEqual(set1, set2, "Set");
      set1.destroy();
      ToolkitSet set3 = toolkit.getSet("TestSet", null);
      assertNotEqual(set1, set3, "Set");

    }

    private void assertNotEqual(Object object1, Object object2, String type) {
      if (object1 == object2) {
        Assert.fail("Got same references while calling Toolkit.get" + type + " with same name after Destroying once");
      } else {
        debug("Got Different References while calling Toolkit.get" + type
              + " with same name after Destroying,Test for " + type + " Passed");
      }
    }

    private void assertEqual(Object object1, Object object2, String type) {
      if (object1 != object2) {
        Assert.fail("Got different deferences while calling Toolkit.get" + type + " with same name");
      } else {
        debug("Got same references while calling Toolkit.get" + type + " with same name");
      }
    }
  }

}
