/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Arrays;

public abstract class CacheEvictorTestApp extends AbstractErrorCatchingTransparentApp {

  public CacheEvictorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    System.out.println(Arrays.asList(getCacheManger().getCacheNames()));

    try {
      testIsElementInMemory();
      testIsKeyInCache();
      testIsValueInCache();
      testElementExpired();
      testRemove();

      testIdleAndTimeToLive("cache1", 5, 10, 20);
      testIdleAndTimeToLive("cache2", 10, 0, 20);
      testIdleAndTimeToLive("cache3", 0, 10, 20);
      testIdleAndTimeToLive("cache4", 0, 0, 20);

      barrier();
    } finally {
      getCacheManger().shutdown();
    }

  }

  protected abstract int barrier() throws Exception;

  protected abstract CacheManager getCacheManger();

  private void testIsElementInMemory() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    populateCache(cache);
    barrier();

    Assert.assertTrue(cache.isElementInMemory("k1"));
    Assert.assertTrue(cache.isElementInMemory("k2"));
    Assert.assertTrue(cache.isElementInMemory("k3"));
  }

  private void populateCache(Cache cache) throws Exception {
    if (barrier() == 0) {
      cache.removeAll();
      cache.put(new Element("k1", "v1"));
      cache.put(new Element("k2", "v2"));
      cache.put(new Element("k3", "v3"));
    }
  }

  private void testIsKeyInCache() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    populateCache(cache);
    barrier();

    Assert.assertTrue(cache.isKeyInCache("k1"));
    Assert.assertTrue(cache.isKeyInCache("k2"));
    Assert.assertTrue(cache.isKeyInCache("k3"));
  }

  private void testIsValueInCache() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    populateCache(cache);
    barrier();

    Assert.assertTrue(cache.isValueInCache("v1"));
    Assert.assertTrue(cache.isValueInCache("v2"));
    Assert.assertTrue(cache.isValueInCache("v3"));
  }

  private void testRemove() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    populateCache(cache);

    if (barrier() == 0) {
      Assert.assertTrue(cache.remove("k1"));
      Assert.assertTrue(cache.remove("k2"));
    }
    barrier();

    Assert.assertFalse(cache.isElementInMemory("k1"));
    Assert.assertFalse(cache.isElementInMemory("k2"));
    Assert.assertTrue(cache.isElementInMemory("k3"));

    Assert.assertFalse(cache.remove("k1"));
    Assert.assertFalse(cache.remove("k2"));

  }

  private void testElementExpired() throws Exception {
    Cache cache = getCacheManger().getCache("sampleCache1");
    populateCache(cache);
    barrier();

    Element e1 = cache.get("k1");
    Element e2 = cache.get("k2");
    Element e3 = cache.get("k3");
    long timeout = System.currentTimeMillis() + (40 * 1000);
    while (System.currentTimeMillis() < timeout) {
      cache.get("k1");
      cache.get("k2");
      Thread.sleep(100);
    }

    // k3,v3 should expire due to timeToIdleSeconds=15
    System.out.println(cache);
    System.out.println(cache.get("k1"));
    System.out.println(cache.get("k2"));
    System.out.println(cache.get("k3"));

    assertExpired(cache, e3);
    assertInCache(cache, e1);
    assertInCache(cache, e2);

    timeout = System.currentTimeMillis() + (80 * 1000);
    while (System.currentTimeMillis() < timeout) {
      cache.get("k1");
      Thread.sleep(100);
    }

    System.out.println(cache.get("k1"));
    System.out.println(cache.get("k2"));
    System.out.println(cache.get("k3"));

    // both (k1,v2) and (k2,v2) should expired due to timeToLiveSeconds=60
    assertExpired(cache, e1);
    assertExpired(cache, e2);
    assertExpired(cache, e3);
  }

  private void testIdleAndTimeToLive(String cacheName, int ttl, int idle, int sleep) throws Exception {
    if (barrier() == 0) {
      getCacheManger().removalAll();
      Cache newCache = new Cache(cacheName, 3, MemoryStoreEvictionPolicy.LFU, false, ".", false, ttl, idle, false, 120,
                                 null, null, 100);
      getCacheManger().addCache(newCache);
      System.out.println(newCache);
    }

    barrier();

    Cache cache = getCacheManger().getCache(cacheName);
    populateCache(cache);
    barrier();

    System.out.println(cache.get("k1"));
    System.out.println(cache.get("k2"));
    System.out.println(cache.get("k3"));

    Element e1 = cache.get("k1");
    Element e2 = cache.get("k2");
    Element e3 = cache.get("k3");

    assertInCache(cache, e1);
    assertInCache(cache, e2);
    assertInCache(cache, e3);

    Thread.sleep(sleep * 1000);

    if (idle == 0 && ttl == 0) { // nothing expired ever
      Assert.assertNotNull(cache.get("k1"));
      Assert.assertNotNull(cache.get("k2"));
      Assert.assertNotNull(cache.get("k3"));
    } else {
      Assert.assertNull(cache.get("k1"));
      Assert.assertNull(cache.get("k2"));
      Assert.assertNull(cache.get("k3"));
    }
  }

  private void assertInCache(Cache cache, Element e) throws Exception {
    Assert.assertTrue("Should be in cache", cache.isKeyInCache(e.getKey()));
    Assert.assertTrue("Should be in memory", cache.isElementInMemory(e.getKey()));
    Assert.assertFalse("Should not expire", cache.isExpired(e));
  }

  private void assertExpired(Cache cache, Element e) throws Exception {
    Assert.assertFalse("Should not be in cache", cache.isKeyInCache(e.getKey()));
    Assert.assertFalse("Should not be in memory", cache.isElementInMemory(e.getKey()));
    Assert.assertTrue("Should expired", cache.isExpired(e));
  }
}
