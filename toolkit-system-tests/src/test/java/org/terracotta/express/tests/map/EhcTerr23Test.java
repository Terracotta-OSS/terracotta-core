/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class EhcTerr23Test extends AbstractToolkitTestBase {

  public EhcTerr23Test(TestConfig testConfig) {
    super(testConfig, EhcTerr23TestClient.class, EhcTerr23TestClient.class);
  }

  public static class EhcTerr23TestClient extends ClientBase {
    private static final int        CLIENT_COUNT       = 2;
    private static final int        NUMBER_OF_ELEMENTS = 5;
    private static final long       TTI                = TimeUnit.SECONDS.toMillis(20);
    private volatile ToolkitBarrier barrier;
    private final int               LOOP_COUNT         = 5;
    private volatile Throwable      e;

    public EhcTerr23TestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      barrier = toolkit.getBarrier("test-EhcTerr23TestClient", CLIENT_COUNT);

      int index = barrier.await();
      ToolkitCache cache = null;
      cache = createCache(toolkit);
      int loopIteration = 0;
      int start = 0;
      while (loopIteration < LOOP_COUNT) {
        barrier.await();

        putSomeElements(cache, index, start);

        barrier.await();

        getSomeElements(cache, start);

        barrier.await();

        Thread.sleep((TTI) / 2);

        Thread updateThread = new Thread(getUpdateRunnable(start, cache, index));
        updateThread.setName("Update thread -- " + index);
        updateThread.start();

        // create remove thread
        Thread removeThread = new Thread(getRemoveRunnable(start, cache, index));
        removeThread.setName("Remove thread -- " + index);
        removeThread.start();

        updateThread.join();
        removeThread.join();
        // join remove thread
        if (e != null) { throw new AssertionError(e); }

        loopIteration++;
        start = start + NUMBER_OF_ELEMENTS;

        System.out.println("Completed " + loopIteration);
      }

    }

    private void putSomeElements(ToolkitCache cache, int index, int start) {
      if (index == 0) {
        for (int i = start; i < (start + NUMBER_OF_ELEMENTS); i++) {
          cache.put(i, i);
        }
      }
    }

    private Runnable getRemoveRunnable(final int start, final ToolkitCache cache, final int index) {
      return new Runnable() {
        @Override
        public void run() {
          try {
            removeElements(start, cache, index);
          } catch (Throwable error) {
            EhcTerr23TestClient.this.e = error;
            error.printStackTrace();
          }
        }

      };
    }

    private Runnable getUpdateRunnable(final int start, final ToolkitCache cache, final int index) {
      return new Runnable() {

        @Override
        public void run() {
          try {
            updateTTI(start, cache, index);
          } catch (Throwable error) {
            EhcTerr23TestClient.this.e = error;
            error.printStackTrace();
          }
        }
      };
    }

    private void removeElements(final int start, final ToolkitCache cache, final int index) {
      if (index == 0) {
        for (int i = start; i < (start + NUMBER_OF_ELEMENTS); i++) {
          System.err.println("Trying to remove " + i + " for index " + i);
          cache.remove(i);
        }
      }
    }

    private void updateTTI(int start, ToolkitCache cache, int index) throws Throwable {
      if (index == 0) {
        for (int i = start; i < (start + NUMBER_OF_ELEMENTS); i++) {
          System.err.println("Trying to update " + i + " for index " + i);
          cache.get(i);
        }
      }
    }

    private void getSomeElements(ToolkitCache cache, int start) {
      for (int i = start; i < (start + NUMBER_OF_ELEMENTS); i++) {
        Assert.assertNotNull(cache.get(i));
      }
    }

    private ToolkitCache createCache(Toolkit toolkit) {
      ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
      builder.maxTTISeconds(20);
      return toolkit.getCache("cache-TTI", builder.build(), null);
    }

  }
}
