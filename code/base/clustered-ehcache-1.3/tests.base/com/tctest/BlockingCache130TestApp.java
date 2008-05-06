/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.CacheManager;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.TIMUtil;

public class BlockingCache130TestApp extends BlockingCacheTestApp {
  private CyclicBarrier barrier;
  private CacheManager  cacheManager;
  
  public BlockingCache130TestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
    cacheManager = CacheManager.create(getClass().getResource("cache-evictor-test.xml"));
  }

  protected int barrier() throws Exception {
    return barrier.barrier();
  }

  protected CacheManager getCacheManger() {
    return cacheManager;
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addModule(TIMUtil.EHCACHE_1_3, TIMUtil.getVersion(TIMUtil.EHCACHE_1_3));

    String testClass = BlockingCache130TestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cacheManager", "cacheManager");

    new CyclicBarrierSpec().visit(visitor, config);
  }
}
