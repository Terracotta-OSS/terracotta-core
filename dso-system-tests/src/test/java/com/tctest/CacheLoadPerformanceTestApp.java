/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CacheLoadPerformanceTestApp extends AbstractTransparentApp {
  private final Map        cache       = new HashMap();
  private final static int CACHE_COUNT = 8;
  private final static int ENTRIES     = 500;
  private final static int BATCH_SIZE  = 100;

  public CacheLoadPerformanceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    Random r = new Random();
    synchronized (cache) {
      for (int i = 0; i < CACHE_COUNT; i++) {
        cache.put(Integer.valueOf(i), new HashMap());
      }
    }
    long start = System.currentTimeMillis();
    int added = 0;
    while (added < ENTRIES) {
      synchronized (cache) {
        for (int i = 0; i < BATCH_SIZE; i++) {
          Integer cid = Integer.valueOf(r.nextInt(CACHE_COUNT));
          Map m = (Map) cache.get(cid);
          m.put(new TestKey(), new TestValue());
          added++;
        }
      }
    }
    System.out.println("Total time:" + (System.currentTimeMillis() - start));
  }

  @SuppressWarnings("unused")
  private class TestKey {
    public Object o = new Object();
    public String s = "Steve " + System.currentTimeMillis();
  }

  @SuppressWarnings("unused")
  private class TestValue {
    public Object o = new Object();
    public String s = "Steve " + System.currentTimeMillis();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CacheLoadPerformanceTestApp.class.getName();
    String methodExpression = "* " + testClass + ".*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("cache", "cache");
    config.addIncludePattern(TestKey.class.getName());
    config.addIncludePattern(TestValue.class.getName());
  }

}
