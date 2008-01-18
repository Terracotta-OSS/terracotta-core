/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.management.CacheMBean;
import net.sf.ehcache.management.CacheManagerMBean;
import net.sf.ehcache.management.CacheStatistics;
import net.sf.ehcache.management.ManagementService;
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

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

public abstract class EhcacheJMXTestBase extends TransparentTestBase {

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(2);
    tt.initializeTestRunner();
  }

  protected abstract Class getApplicationClass();

  public static class BaseApp extends AbstractErrorCatchingTransparentApp {
    private final CyclicBarrier barrier;
    private final CacheManager  cacheManager;

    public BaseApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
      cacheManager = CacheManager.create();
    }

    protected void runTest() throws Throwable {
      final int index = barrier.barrier();

      if (index == 0) {
        synchronized (cacheManager) {
          cacheManager.setName("myCacheManager");
        }
        cacheManager.addCache(makeCache("cache1"));
        cacheManager.addCache(makeCache("cache2"));

        populate(cacheManager.getCache("cache1"));
        populate(cacheManager.getCache("cache2"));
      }

      barrier.barrier();

      MBeanServer mbs = MBeanServerFactory.newMBeanServer();
      ManagementService.registerMBeans(cacheManager, mbs, true, true, true, true);

      CacheManagerMBean mbean = (CacheManagerMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbs, new ObjectName("net.sf.ehcache:type=CacheManager,name=myCacheManager"),
                            CacheManagerMBean.class, false);

      // verify the expected cache names
      Set expectedNames = new HashSet();
      expectedNames.add("cache1");
      expectedNames.add("cache2");
      String[] cacheNames = mbean.getCacheNames();

      for (int i = 0; i < cacheNames.length; i++) {
        String name = cacheNames[i];
        boolean removed = expectedNames.remove(name);
        if (!removed) { throw new AssertionError(name + " not returned from mbean"); }
      }
      if (!expectedNames.isEmpty()) throw new AssertionError("remaing names: " + expectedNames);

      Assert.assertEquals("STATUS_ALIVE", mbean.getStatus());
      Assert.assertEquals(2, mbean.getCaches().size());

      verify(mbean, "cache1", 2);
      verify(mbean, "cache2", 2);

      verifyHits(mbean, "cache1", 0);
      verifyHits(mbean, "cache2", 0);
      Cache cache1 = cacheManager.getCache("cache1");
      cache1.get("key1");
      cache1.get("key2");
      cache1.get("key2");

      barrier.barrier();

      verifyHits(mbean, "cache1", 3);
      verifyHits(mbean, "cache2", 0);

      barrier.barrier();

      if (index == 0) {
        mbean.getCache("cache1").removeAll();
      }

      barrier.barrier();

      verify(mbean, "cache1", 0);
      verify(mbean, "cache2", 2);

      barrier.barrier();

      if (index == 0) {
        mbean.clearAll();
      }

      barrier.barrier();

      verify(mbean, "cache1", 0);
      verify(mbean, "cache2", 0);
    }

    private void verifyHits(CacheManagerMBean mbean, String name, long hits) {
      CacheMBean cacheMbean = mbean.getCache(name);
      CacheStatistics stats = cacheMbean.getStatistics();
      Assert.assertEquals(hits, stats.getCacheHits());
    }

    private void verify(CacheManagerMBean mbean, String name, long size) {
      CacheMBean cacheMbean = mbean.getCache(name);
      CacheStatistics stats = cacheMbean.getStatistics();
      Assert.assertEquals(size, stats.getObjectCount());
      Assert.assertEquals("STATUS_ALIVE", cacheMbean.getStatus());
    }

    private static void populate(Cache cache) {
      cache.put(new Element("key1", "value1"));
      cache.put(new Element("key2", "value2"));
    }

    private static Ehcache makeCache(String name) {
      // never expiring, unlimited size cache
      return new Cache(name, Integer.MAX_VALUE, false, true, 0, 0);
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

      final String testClass = BaseApp.class.getName();
      final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("barrier", "barrier");

      new CyclicBarrierSpec().visit(visitor, config);
    }

  }

}
