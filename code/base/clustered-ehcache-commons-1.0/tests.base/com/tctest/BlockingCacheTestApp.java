/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Arrays;

/*
 * Basic sanity test for Blocking cache.
 */
public abstract class BlockingCacheTestApp extends AbstractErrorCatchingTransparentApp {

  public BlockingCacheTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    System.out.println(Arrays.asList(getCacheManger().getCacheNames()));

    try {
      testBlockingCache();

      barrier();
    } finally {
      getCacheManger().shutdown();
    }

  }

  protected abstract int barrier() throws Exception;

  protected abstract CacheManager getCacheManger();

  private void testBlockingCache() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    BlockingCache bCache = new BlockingCache(cache);
    populateCache(bCache);
    barrier();

    Assert.assertEquals(new Element("k1", "v1"), bCache.get("k1"));
    Assert.assertEquals(new Element("k2", "v2"), bCache.get("k2"));
    Assert.assertEquals(new Element("k3", "v3"), bCache.get("k3"));
    Assert.assertTrue(bCache.isElementInMemory("k1"));
    Assert.assertTrue(bCache.isElementInMemory("k2"));
    Assert.assertTrue(bCache.isElementInMemory("k3"));
  }

  private void populateCache(BlockingCache cache) throws Exception {
    if (barrier() == 0) {
      cache.removeAll();
      cache.put(new Element("k1", "v1"));
      cache.put(new Element("k2", "v2"));
      cache.put(new Element("k3", "v3"));
    }
  }


}
