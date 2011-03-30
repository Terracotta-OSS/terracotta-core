/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

/**
 * This test was written specifically to expose a dead lock in sleepcat in persistence map
 */
public class MapClearDeadLocksSleepycatTestApp extends AbstractTransparentApp {

  private static final int    LOOP_COUNT = 10000;

  private static final int    MAP_SIZE   = 28;

  private final Map           root       = new HashMap();

  private final CyclicBarrier barrier;

  public MapClearDeadLocksSleepycatTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int idx = barrier.barrier();
      if (idx == 0) {
        initialize();
      }
      barrier.barrier();
      testModifyAndClear(idx);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testModifyAndClear(int idx) {
    int debugPrint = 0;
    for (int i = 0; i < LOOP_COUNT; i++) {
      // This synchronization is there only to create transaction boundry and to not throw unlocked shared object
      // exception. The data is partitioned so that threads shouldnt contend.
      if (idx % 2 == 0) {
        iterateForwards(idx, i);
      } else {
        iterateBackwards(idx, i);
      }
      if (debugPrint++ % 100 == 0) {
        System.err.println(Thread.currentThread().getName() + " : Map Iteration count : " + i);
      }
    }
  }

  private void iterateBackwards(int idx, int i) {
    synchronized (Integer.valueOf(idx)) {
      for (int j = MAP_SIZE - 1; j >= 0; j--) {
        if (j % getParticipantCount() == idx) {
          Map m = getMapReadOnly(j);
          modifyAndClear(m, i);
        }
      }
    }
  }

  private void iterateForwards(int idx, int i) {
    synchronized (Integer.valueOf(idx)) {
      for (int j = 0; j < MAP_SIZE; j++) {
        if (j % getParticipantCount() == idx) {
          Map m = getMapReadOnly(j);
          modifyAndClear(m, i);
        }
      }
    }
  }

  private void modifyAndClear(Map m, int i) {
    if (i % 5 == 0) {
      m.clear();
    } else {
      m.put(Long.valueOf(i), "String : " + i);
      m.put(Long.valueOf(i++), "String : " + i);
      m.put(Long.valueOf(i++), "String : " + i);
      m.put(Long.valueOf(i++), "String : " + i);
      m.put(Long.valueOf(i++), "String : " + i);
    }
  }

  private Map getMapReadOnly(int idx) {
    synchronized (root) {
      Integer index = Integer.valueOf(idx);
      return (Map) root.get(index);
    }
  }

  private void initialize() {
    synchronized (root) {
      for (int i = 0; i < MAP_SIZE; i++) {
        root.put(Integer.valueOf(i), new HashMap());
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    visitL1DSOConfig(visitor, config, new HashMap());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config, Map optionalAttributes) {

    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    addWriteAutolock(config, "* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = MapClearDeadLocksSleepycatTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    String writeAllowdMethodExpression = "* " + testClass + "*.*(..)";
    addWriteAutolock(config, writeAllowdMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }

  private static void addWriteAutolock(DSOClientConfigHelper config, String methodPattern) {
    config.addWriteAutolock(methodPattern);
  }

}
