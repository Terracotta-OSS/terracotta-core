/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

public class EnterpriseToolkitNotCreatedTest extends AbstractToolkitTestBase {

  public EnterpriseToolkitNotCreatedTest(TestConfig testConfig) {
    super(testConfig, EnterpriseToolkitNotCreatedTestClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public static class EnterpriseToolkitNotCreatedTestClient extends ClientBase {

    public EnterpriseToolkitNotCreatedTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new EnterpriseToolkitNotCreatedTestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Got toolkit instance of class: " + toolkit.getClass().getName());
      Assert.assertFalse(toolkit.getClass().getName().endsWith(".EnterpriseTerracottaToolkit"));
      Assert.assertTrue(toolkit.getClass().getName().endsWith(".TerracottaToolkit"));

      ToolkitCache<String, Serializable> map = toolkit.getCache("testCache", null);
      Assert.assertTrue(map instanceof ToolkitCacheInternal);
      try {
        map.createQueryBuilder();
        fail("Creating search builder with oss toolkit should fail");
      } catch (UnsupportedOperationException e) {
        System.out.println("Got expected exception: " + e);
        e.printStackTrace();
      }
    }
  }

}
