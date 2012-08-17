/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.concurrent.Callable;

public class ToolkitCachePropertiesTestClient extends ClientBase {
  private ToolkitCache cache;
  private int          index;
  private ToolkitBarrier barrier;
  private Configuration  config;
  public ToolkitCachePropertiesTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    setDs(toolkit);
    index = barrier.await();
    if (index == 0) {
      cache = toolkit.getCache("myCache", getCacheConfigWithlocalCacheEnabled(true), null);
  }
 else {
      cache = toolkit.getCache("myCache", getCacheConfigWithlocalCacheEnabled(false), null);
    }
    barrier.await();
    if (index == 1) {
      cache.put(1, 1);
    }
    barrier.await();
    if (index == 0) {
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          ToolkitCacheInternal toolkitCacheInternal = (ToolkitCacheInternal) cache;
          return (toolkitCacheInternal.localSize() == 0);
        }
      });

    }
    barrier.await();

}

  private Configuration getCacheConfigWithlocalCacheEnabled(boolean var) {

      ToolkitCacheConfigBuilder toolkitCacheConfigBuilder = new ToolkitCacheConfigBuilder();
    toolkitCacheConfigBuilder.localCacheEnabled(var);
    toolkitCacheConfigBuilder.consistency(Consistency.EVENTUAL);
      config = toolkitCacheConfigBuilder.build();

      return config;

  }

  private void setDs(Toolkit toolkit) {
    barrier = toolkit.getBarrier("myBarrier", 2);

  }
}
