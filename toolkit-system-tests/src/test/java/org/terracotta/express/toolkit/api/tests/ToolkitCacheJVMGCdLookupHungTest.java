/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;

import com.tc.test.config.model.TestConfig;

import java.lang.ref.WeakReference;

/**
 * Test for DEV-8006
 */
public class ToolkitCacheJVMGCdLookupHungTest extends AbstractToolkitTestBase {

  public ToolkitCacheJVMGCdLookupHungTest(TestConfig testConfig) {
    super(testConfig, ToolkitCacheJVMGCdLookupHungTestClient.class);
    disableTest();
  }
  
  public static class ToolkitCacheJVMGCdLookupHungTestClient extends ClientBase {
    WeakReference<ToolkitCache> weakCache;

    public ToolkitCacheJVMGCdLookupHungTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      doSomePutsOnCache(toolkit);
      while (weakCache.get() != null) {
        Thread.sleep(1000);
        System.err.println("Sleeping for 1 sec and waiting for cache to be GC'd");
        for (int i = 0; i < 5; i++) {
          System.gc();
        }
      }

      System.err.println(toolkit.getCache("myCache", null).get("1"));
    }

    private void doSomePutsOnCache(Toolkit toolkit) {
      ToolkitCache cache = toolkit.getCache("myCache", null);
      cache.put("1", "1");

      weakCache = new WeakReference<ToolkitCache>(cache);
    }
  }
}
