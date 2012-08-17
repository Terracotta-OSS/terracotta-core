/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

public class ReaderTestClient extends AbstractClusteredMapMultiThreadTestClient {

  public ReaderTestClient(String[] args) {
    super(Consistency.valueOf(System.getProperty(CONSISTENCY)), args);
  }

  @Override
  public void initThreads(ToolkitStore map, AtomicReference<Throwable> error) {
    ReaderThread[] readers = new ReaderThread[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; ++i) {
      readers[i] = new ReaderThread(i * ELEMENTS_FOR_ONE_THREAD, (i + 1) * ELEMENTS_FOR_ONE_THREAD, map, error);
    }
    threads = readers;
  }

  private class ReaderThread extends Thread {
    private final int                        startIndex;
    private final int                        endIndex;
    private final ToolkitStore               map;
    private final AtomicReference<Throwable> error;

    public ReaderThread(int startIndex, int endIndex, ToolkitStore map, AtomicReference<Throwable> error) {
      super("Reader-thread-" + startIndex + "-" + endIndex);
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.map = map;
      this.error = error;
    }

    @Override
    public void run() {
      try {
        barrier.await();
        testBasic();
        barrier.await();
      } catch (Throwable t) {
        error.set(t);
      }
    }

    private void verifyMap() {
      Set keySet = map.keySet();
      // Set entrySet = map.entrySet();
      Collection values = map.values();
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertTrue(map.containsKey(getKey(i)));
        Assert.assertTrue(keySet.contains(getKey(i)));
        Assert.assertTrue(values.contains(getValue(i)));
      }
    }

    private void verifyGet() {
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertEquals(map.get(getKey(i)), getValue(i));
      }
    }

    private void verifyGetAll() {
      Set getKeys = new HashSet();
      for (int i = startIndex; i < endIndex; ++i) {
        getKeys.add(getKey(i));
      }
      Map<String, String> getAll = map.getAll(getKeys);
      Set keySet = getAll.keySet();
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertTrue(getAll.containsKey(getKey(i)));
        Assert.assertTrue(keySet.contains(getKey(i)));
        Assert.assertEquals(getValue(i), getAll.get(getKey(i)));
      }
    }

    private void verifyReplacedMap() {
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertEquals(map.get(getKey(i)), getReplacedValue(i));
      }
      Set keySet = map.keySet();
      Collection values = map.values();
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertTrue(map.containsKey(getKey(i)));
        Assert.assertTrue(keySet.contains(getKey(i)));
        Assert.assertTrue(values.contains(getReplacedValue(i)));
      }
    }

    private void verifyAfterRemoveMap() {
      Assert.assertTrue(map.isEmpty());
      Assert.assertEquals(0, map.size());
      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertNull(map.get(getKey(i)));
      }
    }

    private void verifyDestroyMap() {
      Assert.assertTrue(map.isDestroyed());
    }

    private void testBasic() throws InterruptedException, BrokenBarrierException {
      barrier.await();
      verifyMap();
      verifyGet();
      barrier.await();// done with populate and verify

      barrier.await();
      verifyReplacedMap();
      barrier.await();// done with replace and verify

      barrier.await();
      verifyBasic(map);
      barrier.await(); // done with clear and verify

      barrier.await();
      verifyGet();
      verifyGetAll();
      barrier.await();// done with putAll and verify getAll

      barrier.await();
      verifyAfterRemoveMap();
      barrier.await();// done with remove and verify

      barrier.await();
      verifyDestroyMap();
      barrier.await();// done with destroy and verify
    }
  }

}
