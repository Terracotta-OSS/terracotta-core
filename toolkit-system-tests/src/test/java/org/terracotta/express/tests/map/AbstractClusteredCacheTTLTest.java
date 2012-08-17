/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class AbstractClusteredCacheTTLTest extends AbstractToolkitTestBase {

  public AbstractClusteredCacheTTLTest(TestConfig testConfig,
                                       Class<? extends AbstractClusteredCacheTTLTestClient> clientClass) {
    super(testConfig, clientClass, clientClass);
  }

  public abstract static class AbstractClusteredCacheTTLTestClient extends ClientBase {

    private static final int MAX_TTL_SECONDS = 5;

    public AbstractClusteredCacheTTLTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      debug("Running test with consistency: " + getConsistency());
      ToolkitCache cache = toolkit.getCache("test-cache", new ToolkitCacheConfigBuilder()
          .maxTTLSeconds(MAX_TTL_SECONDS).consistency(getConsistency()).build(), null);

      int numElements = 10;
      int index = waitForAllClients();
      if (index == 0) {
        for (int i = 0; i < numElements; i++) {
          cache.put(getKey(i), getValue(i));
        }
      }
      waitForAllClients();
      for (int i = 0; i < numElements; i++) {
        Object actualValue = cache.get(getKey(i));
        debug("Got actual value: " + actualValue);
        Assert.assertEquals(getValue(i), actualValue);
      }

      final long sleepMillis = TimeUnit.SECONDS.toMillis((long) (MAX_TTL_SECONDS * 1.5));
      debug("Sleeping for " + sleepMillis + " millis...");
      Thread.sleep(sleepMillis);
      debug("After sleep, checking null values");
      for (int i = 0; i < numElements; i++) {
        Object actualValue = cache.get(getKey(i));
        Assert.assertEquals(null, actualValue);
      }
      debug("Test done");
    }

    protected String getValue(int i) {
      return "value-" + i;
    }

    protected String getKey(int i) {
      return "key-" + i;
    }

    protected abstract Consistency getConsistency();
  }

  public static class ClusteredCacheTTLTestStrongClient extends AbstractClusteredCacheTTLTestClient {

    public ClusteredCacheTTLTestStrongClient(String[] args) {
      super(args);
    }

    @Override
    protected Consistency getConsistency() {
      return Consistency.STRONG;
    }

  }

  public static class ClusteredCacheTTLTestEventualClient extends AbstractClusteredCacheTTLTestClient {

    public ClusteredCacheTTLTestEventualClient(String[] args) {
      super(args);
    }

    @Override
    protected Consistency getConsistency() {
      return Consistency.EVENTUAL;
    }

  }

}
