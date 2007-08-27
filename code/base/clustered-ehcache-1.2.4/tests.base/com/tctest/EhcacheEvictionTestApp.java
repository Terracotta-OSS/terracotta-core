package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.Field;

public class EhcacheEvictionTestApp extends AbstractErrorCatchingTransparentApp {
  private static final int    NUM_OF_CACHE_ITEMS      = 1000;
  private static final int    TIME_TO_LIVE_IN_SECONDS = 100;

  static final int            EXPECTED_THREAD_COUNT   = 4;

  private final CyclicBarrier barrier;

  private CacheManager        cacheManager;

  /**
   * Test that Ehcache's CacheManger and Cache objects can be clustered.
   * 
   * @param appId
   * @param cfg
   * @param listenerProvider
   */
  public EhcacheEvictionTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  /**
   * Inject Ehcache 1.2.4 configuration, and instrument this test class
   * 
   * @param visitor
   * @param config
   */
  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.addNewModule("clustered-ehcache-1.2.4", "1.0.0");
    config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

    final String testClass = EhcacheEvictionTestApp.class.getName();
    final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cacheManager", "cacheManager");
    new CyclicBarrierSpec().visit(visitor, config);
  }

  protected void runTest() throws Throwable {
    int index = barrier.barrier();
    if (index == 0) {
      // Even though the singleton field of CacheManager is a root, we need to
      // set cacheManager field to be a root and remove the shutdown hook of cache manager
      // before assigning to root. This hack is to bypass the shutdown hook problem of 1.2.4
      // in system test.
      CacheManager cm = CacheManager.getInstance();
      setShutDownHookToNull(cm);
      cacheManager = cm;
    }

    barrier.barrier();

    if (index == 0) {
      addCache("CACHE", TIME_TO_LIVE_IN_SECONDS, TIME_TO_LIVE_IN_SECONDS * 10);
    }

    barrier.barrier();

    runSimplePutTimeToLiveTimeout(index);

    barrier.barrier();

    if (index == 0) {
      addCache("CACHE", TIME_TO_LIVE_IN_SECONDS);
    }

    barrier.barrier();

    runSimplePutTimeoutGet(index);

    if (index == 0) {
      addCache("CACHE", TIME_TO_LIVE_IN_SECONDS * 4);
    }

    barrier.barrier();

    runSimplePutSimpleGet(index);

    runSimplePut(index);

    if (index == 0) {
      shutdownCacheManager();
    }

    barrier.barrier();
    verifyCacheManagerShutdown();
    barrier.barrier();
  }

  private void runSimplePutTimeToLiveTimeout(int index) throws Throwable {
    if (index == 1) {
      doPut();
    }

    barrier.barrier();

    Thread.sleep(TIME_TO_LIVE_IN_SECONDS * 500);

    barrier.barrier();

    doGet();

    barrier.barrier();

    Thread.sleep(TIME_TO_LIVE_IN_SECONDS * 700);

    barrier.barrier();

    doGetNull(index);

    barrier.barrier();
  }

  private void runSimplePutSimpleGet(int index) throws Throwable {
    if (index == 1) {
      doPut();
      barrier.barrier();
    } else {
      barrier.barrier();

      long startGetTime = System.currentTimeMillis();
      doGet();
      long endGetTime = System.currentTimeMillis();

      System.err.println("Time to get " + NUM_OF_CACHE_ITEMS + " items: " + (endGetTime - startGetTime) + " ms.");

    }

    barrier.barrier();
  }

  private void runSimplePut(int index) throws Throwable {
    Cache cache = cacheManager.getCache("CACHE");
    int startKey = index * NUM_OF_CACHE_ITEMS;
    int endKey = startKey + NUM_OF_CACHE_ITEMS;

    long st = System.currentTimeMillis();

    for (int i = startKey; i < endKey; i++) {
      cache.put(new Element("key" + i, "value" + i));
    }

    long en = System.currentTimeMillis();
    System.err.println("Time to put " + NUM_OF_CACHE_ITEMS + " objects into the cache: " + (en - st) + " ms.");

    barrier.barrier();

  }

  private void runSimplePutTimeoutGet(int index) throws Throwable {
    if (index == 1) {
      doPut();
      Thread.sleep((long) (TIME_TO_LIVE_IN_SECONDS * 1.5 * 1000));
      doGetNull(index);
    }

    barrier.barrier();
  }

  private void doPut() throws Throwable {
    Cache cache = cacheManager.getCache("CACHE");
    for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
      cache.put(new Element("key" + i, "value" + i));
    }
  }

  private void doGet() throws Throwable {
    Cache cache = cacheManager.getCache("CACHE");
    for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
      Object o = cache.get("key" + i);
      Assert.assertEquals(new Element("key" + i, "value" + i), o);
      Assert.assertFalse(cache.isExpired(new Element("key" + i, "value" + i)));
    }
  }

  private void doGetNull(int index) throws Throwable {
    Cache cache = cacheManager.getCache("CACHE");
    for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
      Object o = cache.get("key" + i);
      Assert.assertNull("Object supposed to be Null, but is: " + o, o);
      Assert.assertTrue(cache.isExpired(new Element("key" + i, "value" + i)));
    }
  }

  /**
   * Add a cache into the CacheManager.
   * 
   * @param name The name of the cache to add
   * @throws Throwable
   */
  private Cache addCache(final String name, long timeToIdleSeconds) throws Throwable {
    cacheManager.removeCache(name);
    Cache cache = new Cache(name, 2, MemoryStoreEvictionPolicy.LRU, false, null, false, timeToIdleSeconds * 2,
                            timeToIdleSeconds, false, 1, null);
    cacheManager.addCache(cache);

    cache = cacheManager.getCache(name);
    return cache;
  }

  private Cache addCache(final String name, long timeToLiveSeconds, long timeToIdleSeconds) throws Throwable {
    cacheManager.removeCache(name);
    Cache cache = new Cache(name, 2, MemoryStoreEvictionPolicy.LRU, false, null, false, timeToLiveSeconds,
                            timeToIdleSeconds, false, 1, null);

    cacheManager.addCache(cache);

    cache = cacheManager.getCache(name);
    return cache;
  }

  /**
   * Shuts down the clustered cache manager.
   */
  private void shutdownCacheManager() throws Throwable {
    cacheManager.shutdown();
  }

  /**
   * Verify that the clustered cache manager has shut down.
   */
  private void verifyCacheManagerShutdown() {
    Assert.assertEquals(Status.STATUS_SHUTDOWN, cacheManager.getStatus());
  }

  private void setShutDownHookToNull(CacheManager cacheManager) {
    try {
      Field f = CacheManager.class.getDeclaredField("shutdownHook");
      f.setAccessible(true);
      Thread t = (Thread) f.get(cacheManager);
      if (t != null) {
        Runtime.getRuntime().removeShutdownHook(t);
        f.set(cacheManager, null);
      }
    } catch (Exception e) {
      //
    }
  }

}
