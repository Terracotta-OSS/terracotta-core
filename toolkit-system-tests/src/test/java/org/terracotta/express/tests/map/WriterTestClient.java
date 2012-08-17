/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.junit.Assert;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class WriterTestClient extends AbstractClusteredMapMultiThreadTestClient {

  public WriterTestClient(String[] args) {
    super(Consistency.valueOf(System.getProperty(CONSISTENCY)), args);
  }

  @Override
  public void initThreads(ToolkitStore map, AtomicReference<Throwable> error) {
    WriterThread[] writers = new WriterThread[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; ++i) {
      writers[i] = new WriterThread(i * ELEMENTS_FOR_ONE_THREAD, (i + 1) * ELEMENTS_FOR_ONE_THREAD, map, error);
    }
    threads = writers;
  }

  private class WriterThread extends Thread {
    private final int                        startIndex;
    private final int                        endIndex;
    private final ToolkitStore               map;
    private final AtomicReference<Throwable> error;

    public WriterThread(int startIndex, int endIndex, ToolkitStore map, AtomicReference<Throwable> error) {
      super("Writer-thread-" + startIndex + "-" + endIndex);
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

    private void putAll() throws Exception {
      Map<String, String> putMap = new HashMap<String, String>();
      for (int i = startIndex; i < endIndex; ++i) {
        putMap.put(getKey(i), getValue(i));
      }
      map.putAll(putMap);
    }

    private void populateMap() throws Exception {
      for (int i = startIndex; i < endIndex; ++i) {
        map.put(getKey(i), getValue(i));
      }

      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(NUM_ELEMENTS, map.size());
        }
      });

      for (int i = startIndex; i < endIndex; ++i) {
        Assert.assertTrue(map.containsKey(getKey(i)));
      }
    }

    private void replaceMap() throws Exception {
      for (int i = startIndex; i < endIndex; ++i) {
        map.replace(getKey(i), getReplacedValue(i));
      }
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(NUM_ELEMENTS, map.size());
        }
      });
    }

    private void clearMap() throws Exception {
      map.clear();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(0, map.size());
        }
      });
    }

    private void removeFromMap() throws Exception {
      for (int i = startIndex; i < endIndex; ++i) {
        map.remove(getKey(i));
      }
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(0, map.size());
        }
      });
    }

    private void destroyMap() throws Exception {
      map.destroy();
    }

    private void testBasic() throws Exception {
      populateMap();
      debug("Populate done");
      barrier.await();
      barrier.await(); // done with populate and verify

      debug("Replacing map...");
      replaceMap();
      debug("Replacing map done");
      barrier.await();
      barrier.await(); // done with replace and verify

      debug("Clearing map...");
      clearMap();
      debug("Clearing map done");
      barrier.await();
      barrier.await(); // done with clear and verify

      debug("Doing putAll...");
      putAll();
      debug("Doing putAll done");
      barrier.await();
      barrier.await();// done with putAll and verify getAll

      debug("Removing from map...");
      removeFromMap();
      debug("Removing from map done");
      barrier.await();
      barrier.await(); // done with remove and verify

      debug("Destroying map...");
      destroyMap();
      debug("Destroying map done");
      barrier.await();
      barrier.await();// done with destroy and verify

    }
  }
}
