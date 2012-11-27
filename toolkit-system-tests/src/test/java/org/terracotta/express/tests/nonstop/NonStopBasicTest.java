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

import com.tc.test.config.model.TestConfig;

import java.util.Date;

public class NonStopBasicTest extends AbstractToolkitTestBase {

  public NonStopBasicTest(TestConfig testConfig) {
    super(testConfig, NonStopBasicTestClient.class, NonStopBasicTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
    testConfig.getL2Config().setRestartable(true);
  }

  public static class NonStopBasicTestClient extends NonStopClientBase {
    private static final int CLIENT_COUNT           = 2;
    private static final int NUMBER_OF_ELEMENTS     = 2000;
    private static final int MAX_ENTRIES_LOCAL_HEAP = 500;

    public NonStopBasicTestClient(String[] args) {
      super(args);
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

      for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
        cache.get(i);
        System.err.println("Cache class " + cache.getClass());
      }
      System.err.println("Cache size: " + cache.size() + " at " + new Date());
    }

    private ToolkitCache createCache(Toolkit toolkit) {
      ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
      builder.maxCountLocalHeap(MAX_ENTRIES_LOCAL_HEAP);

      return toolkit.getCache("test-cache", builder.build(), Integer.class);
    }
  }
}
