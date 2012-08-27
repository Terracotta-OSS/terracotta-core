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

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LookupHungTTITest extends AbstractToolkitTestBase {

  public LookupHungTTITest(TestConfig testConfig) {
    super(testConfig, LookupHungTTITestClient.class, LookupHungTTITestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class LookupHungTTITestClient extends ClientBase {

    private static final int CLIENT_COUNT           = 2;
    private static final int NUMBER_OF_ELEMENTS     = 2000;
    private static final int MAX_ENTRIES_LOCAL_HEAP = 500;
    private static final int TTI_SECONDS            = 20;
    private static final int TIME_TO_RUN_MINUTES    = 5;

    public LookupHungTTITestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new LookupHungTTITestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitBarrier barrier = toolkit.getBarrier("LookupHungTTITestClient", CLIENT_COUNT);
      int index = barrier.await();
      ToolkitCache cache = null;
      cache = createCache(toolkit);

      if (index == 0) {
        for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
          cache.put(i, i);
        }
      }

      System.err.println("Index 0 Cache size " + cache.size() + " at " + new Date());

      barrier.await();

      long startTime = System.currentTimeMillis();
      while (canRun(startTime)) {
        for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
          cache.get(i);
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(TTI_SECONDS) / 2);
        System.err.println("Cache size: " + cache.size() + " at " + new Date());
      }
    }

    private boolean canRun(long startTime) {
      return startTime + TimeUnit.MINUTES.toMillis(TIME_TO_RUN_MINUTES) > System.currentTimeMillis();
    }

    private ToolkitCache createCache(Toolkit toolkit) {
      ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
      builder.maxTTISeconds(TTI_SECONDS);
      builder.maxCountLocalHeap(MAX_ENTRIES_LOCAL_HEAP);

      return toolkit.getCache("tti-cache", builder.build(), Integer.class);
    }
  }

}
