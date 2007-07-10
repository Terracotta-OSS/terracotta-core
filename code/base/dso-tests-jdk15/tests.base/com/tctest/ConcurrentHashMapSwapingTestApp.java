/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentHashMapSwapingTestApp extends AbstractTransparentApp {
  private static final String     GC_CREATE_NUM_KEY = "gc-create-num";

  private static final int        NUM_OF_PUT        = 2000;
  private static final int        NUM_OF_LOOP       = 5;
  private static final int        MAX_KEY_VALUE     = 1000;

  private final CyclicBarrier     barrier;
  private final ConcurrentHashMap mapRoot           = new ConcurrentHashMap();
  private int                     gcCreateNum;

  public ConcurrentHashMapSwapingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());

    try {
      gcCreateNum = Integer.parseInt(cfg.getAttribute(GC_CREATE_NUM_KEY));
      Assert.assertTrue(gcCreateNum > 0);
    } catch (NumberFormatException e) {
      gcCreateNum = 1;
    }
  }

  public void run() {
    try {
      int index = barrier.await();
      for (int i = 0; i < gcCreateNum; i++) {
        testPutMany(index);
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testPutMany(int index) throws Exception {
    clearMap(index);

    for (int j = 0; j < NUM_OF_LOOP; j++) {
      if (index == 0) {
        for (int i = 0; i < NUM_OF_PUT; i++) {
          Assert.assertEquals((j == 0 && i < MAX_KEY_VALUE) ? i : MAX_KEY_VALUE, mapRoot.size());
          int beforeSize = mapRoot.size();
          int useVal = i % MAX_KEY_VALUE;
          if (i % 100 == 0) System.err.println("Puting -- i: " + i + ", using key: " + useVal);
          Object key = new HashKey(useVal);
          while (System.identityHashCode(key) == key.hashCode()) {
            key = new HashKey(useVal);
          }
          Assert.assertFalse(System.identityHashCode(key) == key.hashCode());
          mapRoot.put(key, new HashValue(useVal));
          int afterSize = mapRoot.size();
          Assert.assertTrue(afterSize >= beforeSize);
          if (i % 100 == 0) System.err.println("beforeSize: " + beforeSize + ", afterSize: " + afterSize);
        }
      }

      barrier.await();
    }

    Assert.assertEquals(MAX_KEY_VALUE, mapRoot.size());

    for (int i = 0; i < MAX_KEY_VALUE; i++) {
      int useVal = i % MAX_KEY_VALUE;
      if (i % 100 == 0) System.err.println("Getting key: " + useVal);
      Object key = new HashKey(useVal);
      if (key.hashCode() == System.identityHashCode(key)) {
        String assertionMsg = "Getting key: " + useVal + ", hashCode: " + key.hashCode() + ", identityHashCode: "
                              + System.identityHashCode(key);
        throw new AssertionError(assertionMsg);
      }
      Assert.assertEquals(new HashValue(useVal), mapRoot.get(key));
    }

    barrier.await();
  }

  private void clearMap(int index) throws Exception {
    if (index == 0) {
      mapRoot.clear();
    }

    barrier.await();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcurrentHashMapSwapingTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("mapRoot", "mapRoot");
  }

  private static class HashKey {
    private int i;

    public HashKey(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }

    public int hashCode() {
      return i;
    }

    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashKey)) return false;
      return ((HashKey) obj).i == i;
    }
  }

  private static class HashValue {
    private int i;

    public HashValue(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }

    public int hashCode() {
      return i;
    }

    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashValue)) return false;
      return ((HashValue) obj).i == i;
    }

    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

}
