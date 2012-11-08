/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.NonStopClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.nonstop.NonStopConfigBuilder;
import org.terracotta.toolkit.nonstop.NonStopException;

import com.tc.test.config.model.TestConfig;

import java.util.Date;

import junit.framework.Assert;

public class NonStopFunctionalityCheckTest extends AbstractToolkitTestBase {

  public NonStopFunctionalityCheckTest(TestConfig testConfig) {
    super(testConfig, NonStopFunctionalityCheckTestClient.class, NonStopFunctionalityCheckTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopFunctionalityCheckTestClient extends NonStopClientBase {
    private static final int  CLIENT_COUNT            = 2;
    private static final int  NUMBER_OF_ELEMENTS      = 10;
    private static final int  MAX_ENTRIES_LOCAL_HEAP  = 500;
    private static final long NON_STOP_TIMEOUT_MILLIS = 10000;

    public NonStopFunctionalityCheckTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new NonStopFunctionalityCheckTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBarrier barrier = toolkit.getBarrier("testBarrier", CLIENT_COUNT);
      int index = barrier.await();
      ToolkitCache cache = null;
      cache = createCache(toolkit);

      if (index == 0) {
        for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
          cache.put(i, i);
        }
        System.err.println("Cache size " + cache.size() + " at " + new Date());
      }

      barrier.await();

      if (index == 1) {
        for (int i = 0; i < NUMBER_OF_ELEMENTS / 2; i++) {
          long time = System.currentTimeMillis();
          try {
            Assert.assertNotNull(cache.get(i));
          } catch (NonStopException e) {
            System.err.println("Time elapsed " + (System.currentTimeMillis() - time) + " , i=" + i);
            throw e;
          }
        }

        makeServerDie();

        for (int i = NUMBER_OF_ELEMENTS / 2; i < NUMBER_OF_ELEMENTS; i++) {
          boolean exceptionOccurred = false;
          long time = System.currentTimeMillis();
          try {
            cache.get(i);
          } catch (NonStopException e) {
            time = System.currentTimeMillis() - time;

            Assert.assertTrue((time > (NON_STOP_TIMEOUT_MILLIS - 500)) && (time < (NON_STOP_TIMEOUT_MILLIS + 2000)));
            System.err.println("Time consumed " + time);

            exceptionOccurred = true;
          }

          Assert.assertTrue(exceptionOccurred);
        }

        restartCrashedServer();
      }
    }

    private void restartCrashedServer() throws Exception {
      getTestControlMbean().reastartLastCrashedServer(0);
    }

    private void makeServerDie() throws Exception {
      getTestControlMbean().crashActiveServer(0);
    }

    private ToolkitCache createCache(Toolkit toolkit) {
      String cacheName = "test-cache";

      new NonStopConfigBuilder().timeoutMillis(NON_STOP_TIMEOUT_MILLIS).apply(toolkit);

      ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
      builder.maxCountLocalHeap(MAX_ENTRIES_LOCAL_HEAP);

      return toolkit.getCache(cacheName, builder.build(), Integer.class);
    }
  }

}
