/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyNonLiteralValueGenerator;
import org.terracotta.express.tests.util.TCInt;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitCacheRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitCacheRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitCacheRejoinTestClient.class, ToolkitCacheRejoinTestClient.class);
    testConfig.getL2Config().setRestartable(false);
  }

  public static class ToolkitCacheRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static int CLINT_COUNT = 2;
    private static int NUM_ELEMENTS = 10;

    public ToolkitCacheRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
      ToolkitBarrier toolkitCacheBarrier = toolkit.getBarrier("toolkitCacheBarrier", CLINT_COUNT);
      int index = toolkitCacheBarrier.await();
      ToolkitCache<String, TCInt> toolkitCache = toolkit.getCache("toolkitCache", TCInt.class);
      toolkitCacheBarrier.await();
      doDebug("client " + index + " starting.. ");

      if (index == 1) {
        doDebug("Adding values before rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          toolkitCache.put(getKey(i), getValue(i));
        }
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }
      debug("client " + index + " done with rejoin");
      toolkitCacheBarrier.await();

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals(getValue(i), toolkitCache.get(getKey(i)));
      }
      toolkitCacheBarrier.await();

      if (index == 1) {
        doDebug("Adding new values after rejoin");
        for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
          toolkitCache.put(getKey(i), getValue(i));
        }
      }
      toolkitCacheBarrier.await();

      doDebug("Asserting new values after rejoin");
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertEquals(getValue(i), toolkitCache.get(getKey(i)));
      }

      toolkitCacheBarrier.await();

      doDebug("getting a fresh blocking queue after rejoin");
      ToolkitCache<String, TCInt> freshToolkitCache = toolkit.getCache("freshToolkitCache",
                                                                                  TCInt.class);
      if (index == 1) {
        doDebug("adding values in fresh blocking queue after rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          freshToolkitCache.put(getKey(i), getValue(i));
        }
        toolkit.waitUntilAllTransactionsComplete();
      }
      toolkitCacheBarrier.await();
      doDebug("asserting fresh cache after rejoin " + freshToolkitCache.size());
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals(getValue(i), freshToolkitCache.get(getKey(i)));
      }
    }

    private String getKey(int i) {
      return (String) keyValueGenerator.getKey(i);
    }

    private TCInt getValue(int i) {
      return (TCInt) keyValueGenerator.getValue(i);
    }

  }
}
