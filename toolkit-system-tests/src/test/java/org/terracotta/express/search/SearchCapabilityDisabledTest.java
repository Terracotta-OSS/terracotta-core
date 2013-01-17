/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.search;

import org.junit.Assert;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import com.tc.test.config.model.TestConfig;

public class SearchCapabilityDisabledTest extends AbstractToolkitTestBase {

  public SearchCapabilityDisabledTest(TestConfig testConfig) {
    super(testConfig, Client.class);
  }

  public static class Client extends ClientBase {

    public Client(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Got toolkit: " + toolkit.getClass().getName());
      Assert.assertNotNull(toolkit.getFeature(ToolkitFeatureType.SEARCH));
      Assert.assertFalse(toolkit.getFeature(ToolkitFeatureType.SEARCH).isEnabled());
      ToolkitCache cache = toolkit.getCache("some-cache", null);
      Assert.assertTrue(cache instanceof ToolkitCacheInternal);
      try {
        cache.createQueryBuilder();
        fail();
      } catch (UnsupportedOperationException e) {
        // expected
        debug("Caught expected exception - " + e);
        // msg -> "Search is supported in enterprise version only"
        Assert.assertTrue(e.getMessage().indexOf("Search") >= 0);
        Assert.assertTrue(e.getMessage().indexOf("support") >= 0);
        Assert.assertTrue(e.getMessage().indexOf("enterprise") >= 0);
        Assert.assertTrue(e.getMessage().indexOf("only") >= 0);
      }
    }

    public static void main(String[] args) {
      new Client(args).run();
    }
  }

}
