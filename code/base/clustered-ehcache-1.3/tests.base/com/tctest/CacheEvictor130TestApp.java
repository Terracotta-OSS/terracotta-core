/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

public class CacheEvictor130TestApp extends CacheEvictorTestApp {
  private CyclicBarrier barrier;
  private CacheManager  cacheManager;
  
  public CacheEvictor130TestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
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
    config.addNewModule("clustered-ehcache-1.3", "1.0.0");

    String testClass = CacheEvictor130TestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cacheManager", "cacheManager");

    new CyclicBarrierSpec().visit(visitor, config);
  }
}
