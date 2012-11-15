/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class DEV8287Test extends AbstractToolkitTestBase {

  public DEV8287Test(TestConfig testConfig) {
    super(testConfig, DEV8287TestClient.class);
    timebombTest("2012-12-01");
  }

  public static class DEV8287TestClient extends ClientBase {
    public DEV8287TestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new DEV8287TestClient(args).run();
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      bug_given_a_cache_with_no_local_cache_when_try_to_insert_10_entries_then_none_are_in_the_local_cache(toolkit);
      // Thread.sleep(TimeUnit.MINUTES.toMillis(5));
    }

    public void bug_given_a_cache_with_no_local_cache_when_try_to_insert_10_entries_then_none_are_in_the_local_cache(Toolkit toolkit) {
      ToolkitCache cache = toolkit.getCache("storeLocalCachedFalse",
                                            new ToolkitCacheConfigBuilder().localCacheEnabled(false).build(),
                                            String.class);
      for (int i = 0; i < 10; i++) {
        cache.put("elem" + i, "value" + i);
      }
      ToolkitCacheInternal<String, String> cacheInternal = (ToolkitCacheInternal<String, String>) cache;
      Assert.assertTrue(cacheInternal.localOnHeapSize() == 0);
      Assert.assertTrue(cacheInternal.size() == 10);
    }
  }

}
