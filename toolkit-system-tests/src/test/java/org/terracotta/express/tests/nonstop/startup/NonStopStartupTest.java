/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop.startup;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.NonStopClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigurationBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopException;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class NonStopStartupTest extends AbstractToolkitTestBase {

  public NonStopStartupTest(TestConfig testConfig) {
    super(testConfig, NonStopStartupTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopStartupTestClient extends NonStopClientBase {
    protected static final int  NUMBER_OF_ELEMENTS      = 10;
    protected static final int  MAX_ENTRIES_LOCAL_HEAP  = 0;
    protected static final long NON_STOP_TIMEOUT_MILLIS = 10000;

    public NonStopStartupTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      toolkit.shutdown();

      Thread.sleep(5000);

      makeServerDie();

      toolkit = createToolkit();

      ToolkitCache<Integer, Integer> cache = null;
      cache = createCache(toolkit);

      boolean exceptionCaught = false;
      try {
        cache.put(1, 1);
      } catch (NonStopException e) {
        exceptionCaught = true;
        e.printStackTrace();
      }
      
      Assert.assertTrue(exceptionCaught);

      Thread.sleep(5000);

      restartCrashedServer();

      Thread.sleep(5000);
    }

    private void restartCrashedServer() throws Exception {
      getTestControlMbean().reastartLastCrashedServer(0);
    }

    private void makeServerDie() throws Exception {
      getTestControlMbean().crashActiveServer(0);
      Thread.sleep(10 * 1000);
    }

    private ToolkitCache createCache(Toolkit toolkit) {
      String cacheName = "test-cache";

      new NonStopConfigurationBuilder().timeoutMillis(NON_STOP_TIMEOUT_MILLIS)
          .nonStopTimeoutBehavior(NonStopTimeoutBehavior.EXCEPTION_ON_TIMEOUT,
                                  NonStopTimeoutBehavior.EXCEPTION_ON_TIMEOUT).apply(toolkit);

      ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
      builder.maxCountLocalHeap(MAX_ENTRIES_LOCAL_HEAP);

      return toolkit.getCache(cacheName, builder.build(), Integer.class);
    }
  }

}
