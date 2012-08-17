/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class CustomTTLTest extends AbstractToolkitTestBase {

  public CustomTTLTest(TestConfig testConfig) {
    super(testConfig, CustomTTLTestClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public static class CustomTTLTestClient extends ClientBase {

    private static final int TTL_SECONDS = 5;

    public CustomTTLTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new CustomTTLTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Got toolkit: " + toolkit);

      System.out.println("Getting map...");
      ToolkitCacheWithMetadata<String, String> cache = (ToolkitCacheWithMetadata<String, String>) toolkit
          .getCache("someMap", String.class);
      System.out.println("Got cache: " + cache.getClass().getName());

      System.out.println("Inserting custom ttl key-value");
      cache.putWithMetaData("key", "value", now(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS, TTL_SECONDS, null);

      String value = cache.get("key");
      System.out.println("Got value: " + value);
      Assert.assertEquals("value", value);

      Thread.sleep(TTL_SECONDS * 2000);

      value = cache.get("key");
      System.out.println("Got value: " + value);
      Assert.assertNull("expected null - " + value, value);

    }

    private int now() {
      return (int) (System.currentTimeMillis() / 1000);
    }
  }

}
