/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.ServerCrasher;
import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class AbstractClusteredCacheTTICrashTest extends AbstractExpressActivePassiveTest {

  public AbstractClusteredCacheTTICrashTest(TestConfig testConfig,
                                            Class<? extends AbstractClusteredCacheTTICrashTestClient> clientClass) {
    super(testConfig, clientClass, clientClass);
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.toolkit.map.expiration.debug=true");
  }

  public abstract static class AbstractClusteredCacheTTICrashTestClient extends ClientBase {

    private static final int MAX_TTI_SECONDS      = 20;
    private static final int HALF_MAX_TTI_SECONDS = MAX_TTI_SECONDS / 2;

    public AbstractClusteredCacheTTICrashTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      debug("Running test with consistency: " + getConsistency());
      ToolkitCache cache = toolkit.getCache("test-cache", new ToolkitCacheConfigBuilder()
          .maxTTISeconds(MAX_TTI_SECONDS).consistency(getConsistency()).build(), null);

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

      final long halfTTIMillis = TimeUnit.SECONDS.toMillis((HALF_MAX_TTI_SECONDS));

      ServerCrasher crasher = null;
      if (index == 0) {
        crasher = ServerCrasher.crashServerAfterMillis(getTestControlMbean(), halfTTIMillis);
      }

      debug("Sleeping for half tti : " + halfTTIMillis + " millis...");
      Thread.sleep(halfTTIMillis);
      debug("After sleep, touching half values");
      for (int i = 0; i < numElements / 2; i++) {
        Object actualValue = cache.get(getKey(i));
        Assert.assertEquals(getValue(i), actualValue);
      }

      long sleepMillis = (long) (halfTTIMillis * 1.2);
      debug("Sleeping for another 1.2 * half tti : " + sleepMillis + " millis...");
      Thread.sleep(sleepMillis);
      debug("After sleep, half should be expired values");
      for (int i = 0; i < numElements; i++) {
        Object actualValue = cache.get(getKey(i));
        if (i < numElements / 2) {
          Assert.assertEquals(getValue(i), actualValue);
        } else {
          Assert.assertNull(actualValue);
        }
      }

      sleepMillis = (TimeUnit.SECONDS.toMillis(MAX_TTI_SECONDS) * 2);
      debug("Sleeping for another 1.2 * max tti : " + sleepMillis + " millis...");
      Thread.sleep(sleepMillis);
      debug("After sleep, all should be expired");
      for (int i = 0; i < numElements; i++) {
        String key = getKey(i);
        Object actualValue = cache.get(key);
        Assert.assertNull("Element should have been expired: key: " + key + ", value: " + actualValue, actualValue);
      }

      if (index == 0) {
        crasher.join();
        if (!crasher.isSuccess()) { throw new AssertionError("there were some problem crashing the server"); }
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

  public static class ClusteredCacheTTICrashTestStrongClient extends AbstractClusteredCacheTTICrashTestClient {

    public ClusteredCacheTTICrashTestStrongClient(String[] args) {
      super(args);
    }

    @Override
    protected Consistency getConsistency() {
      return Consistency.STRONG;
    }

  }

  public static class ClusteredCacheTTICrashTestEventualClient extends AbstractClusteredCacheTTICrashTestClient {

    public ClusteredCacheTTICrashTestEventualClient(String[] args) {
      super(args);
    }

    @Override
    protected Consistency getConsistency() {
      return Consistency.EVENTUAL;
    }

  }

}
