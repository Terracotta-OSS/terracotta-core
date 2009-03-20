/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PartialCollectionEvictionTestApp extends AbstractErrorCatchingTransparentApp {

  public PartialCollectionEvictionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  private final Map<String, Object> map = new ConcurrentHashMap<String, Object>();

  @Override
  protected void runTest() throws Throwable {
    Manager mgr = ManagerUtil.getManager();
    Field objectManager = mgr.getClass().getDeclaredField("objectManager");
    objectManager.setAccessible(true);
    Evictable cache = (Evictable) objectManager.get(mgr);
    
    final int SIZE = 100;
    for (int i = 0; i < SIZE; i++) {
      map.put("old mapping " + i, new Object());
    }

    DummyCacheStats cs = new DummyCacheStats(SIZE);    
    cache.evictCache(cs);    
    Assert.assertEquals(0, cs.evicted);
    
    for (int i = 0; i < SIZE; i++) {
      map.put("new mapping " + i, new Object());
    }
    
    cache.evictCache(cs);
    Assert.assertEquals(SIZE, cs.evicted);
    
    Set<String> localKeys = ManagerUtil.getManager().getDsoCluster().getKeysForLocalValues(map);
    
    int oldMappingCount = 0;
    for (String s : localKeys) {
      if (s.startsWith("old mapping"))
        oldMappingCount++;
    }
    
    Assert.assertTrue("No Old Mappings Flushed", oldMappingCount < SIZE);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = PartialCollectionEvictionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    spec.addRoot("map", "map");
  }

  static class DummyCacheStats implements CacheStats {
    public final int target;
    public int evicted = 0;

    public DummyCacheStats(int target) {
      this.target = target;
    }
    
    public int getObjectCountToEvict(int currentCount) {
      return Math.max(target - evicted, 0);
    }

    public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC) {
      evicted += evictedCount;
    }
  }
}
