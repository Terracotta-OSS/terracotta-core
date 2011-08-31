/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartialCollectionEvictionTestApp extends AbstractErrorCatchingTransparentApp {

  public PartialCollectionEvictionTestApp(final String appId, final ApplicationConfig cfg,
                                          final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  private final Map<String, Object> map = new HashMap<String, Object>();

  @Override
  protected void runTest() throws Throwable {
    final Manager mgr = ManagerUtil.getManager();
    final Field objectManager = mgr.getClass().getDeclaredField("objectManager");
    objectManager.setAccessible(true);
    final Evictable cache = (Evictable) objectManager.get(mgr);

    final int SIZE = 100;
    synchronized (this.map) {
      for (int i = 0; i < SIZE; i++) {
        this.map.put("old mapping " + i, new Object());
      }
    }

    final DummyCacheStats cs = new DummyCacheStats(SIZE);
    cache.evictCache(cs);
    Assert.assertEquals(0, cs.evicted);

    synchronized (this.map) {
      for (int i = 0; i < SIZE; i++) {
        this.map.put("new mapping " + i, new Object());
      }
    }

    cache.evictCache(cs);
    Assert.assertEquals(SIZE, cs.evicted);

    int flushedOldMappingCount = 0;
    final Collection<Map.Entry> entries = ((TCMap) this.map).__tc_getAllEntriesSnapshot();
    int flushed = 0;
    for (final Map.Entry entry : entries) {
      if (entry.getValue() instanceof ObjectID) {
        if (((String) entry.getKey()).startsWith("old mapping")) {
          flushedOldMappingCount++;
        }
        flushed++;
      }
    }
    System.out.println("Flushed = " + flushed + " old mapping flushed : " + flushedOldMappingCount);

    Assert.assertTrue("No Old Mappings Flushed", flushedOldMappingCount > 0);
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    final String testClass = PartialCollectionEvictionTestApp.class.getName();
    final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    spec.addRoot("map", "map");
    config.addWriteAutolock("* " + testClass + ".runTest()");
  }

  static class DummyCacheStats implements CacheStats {
    public final int target;
    public int       evicted = 0;

    public DummyCacheStats(final int target) {
      this.target = target;
    }

    public int getObjectCountToEvict(final int currentCount) {
      return Math.max(this.target - this.evicted, 0);
    }

    public void objectEvicted(final int evictedCount, final int currentCount, final List targetObjects4GC,
                              final boolean printNewObjects) {
      System.err.println("Evicted : " + evictedCount);
      this.evicted += evictedCount;
    }
  }
}
