/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.restart.system.ObjectDataTestApp;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LinkedHashMapTestApp extends AbstractTransparentApp {
  public static final String  SYNCHRONOUS_WRITE = "synch-write";

  private final MapRoot       root              = new MapRoot();

  private final CyclicBarrier barrier;

  public LinkedHashMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      putTesting();
      getTesting();
      getROTesting();
      unsharedGetTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void clear() throws Exception {
    synchronized (root) {
      root.clear();
    }
    barrier.barrier();
  }

  private void initialize() throws Exception {
    synchronized (root) {
      if (root.getIndex() == 0) {
        root.getMap().putAll(getInitialMapData());
        root.setIndex(root.getIndex() + 1);
      }
    }
    barrier.barrier();
  }

  private Map getInitialMapData() {
    Map map = new HashMap();
    map.put("January", "Jan");
    map.put("February", "Feb");
    map.put("March", "Mar");
    map.put("April", "Apr");
    return map;
  }

  private void getTesting() throws Exception {
    clear();
    initialize();

    LinkedHashMap map = root.getMap();
    synchronized (map) {
      if (root.getIndex() != 0) {
        map.get("February");
        map.get("April");
        root.setIndex(0);
      }
    }

    barrier.barrier();

    LinkedHashMap expect = new LinkedHashMap(10, 0.75f, true);
    expect.putAll(getInitialMapData());
    expect.get("February");
    expect.get("April");

    assertMappings(expect, map);

    barrier.barrier();
  }

  /**
   * The goal of this test is that it will not throw a ReadOnlyException during the get() operation since the
   * LinkedHashMap is unshared.
   */
  private void unsharedGetTesting() throws Exception {
    LinkedHashMap map = new LinkedHashMap(10, 0.75f, true);
    map.putAll(getInitialMapData());
    map.get("February");
    map.get("April");

    barrier.barrier();
  }

  private void getROTesting() throws Exception {
    clear();
    initialize();
    try {
      tryReadOnlyGetTesting();
      throw new AssertionError("I should have thrown an ReadOnlyException.");
    } catch (Throwable t) {
      if (ReadOnlyException.class.getName().equals(t.getClass().getName())) {
        // ignore ReadOnlyException in read only tests.
      } else {
        throw new RuntimeException(t);
      }
    }
    barrier.barrier();
  }

  private void tryReadOnlyGetTesting() {
    LinkedHashMap map = root.getMap();
    synchronized (map) {
      if (root.getIndex() != 0) {
        map.get("February");
        map.get("April");
        root.setIndex(0);
      }
    }
  }

  private void putTesting() throws Exception {
    clear();
    initialize();

    LinkedHashMap expect = new LinkedHashMap(10, 0.75f, true);
    expect.putAll(getInitialMapData());

    assertMappings(expect, root.getMap());

    barrier.barrier();

  }

  void assertMappings(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();
    for (Iterator iExpect = expectEntries.iterator(), iActual = actualEntries.iterator(); iExpect.hasNext();) {
      Assert.assertEquals(iExpect.next(), iActual.next());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    visitL1DSOConfig(visitor, config, new HashMap());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config, Map optionalAttributes) {
    boolean isSynchronousWrite = false;
    if (optionalAttributes.size() > 0) {
      isSynchronousWrite = Boolean.valueOf((String) optionalAttributes.get(ObjectDataTestApp.SYNCHRONOUS_WRITE))
          .booleanValue();
    }

    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    addWriteAutolock(config, isSynchronousWrite, "* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = LinkedHashMapTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(MapRoot.class.getName());

    String writeAllowdMethodExpression = "* " + testClass + "*.*(..)";
    addWriteAutolock(config, isSynchronousWrite, writeAllowdMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }

  private static void addWriteAutolock(DSOClientConfigHelper config, boolean isSynchronousWrite, String methodPattern) {
    if (isSynchronousWrite) {
      config.addSynchronousWriteAutolock(methodPattern);
    } else {
      config.addWriteAutolock(methodPattern);
    }
  }

  private static class MapRoot {
    private final LinkedHashMap map = new LinkedHashMap(10, 0.75f, true);
    private int                 index;

    public MapRoot() {
      index = 0;
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public LinkedHashMap getMap() {
      return map;
    }

    public void clear() {
      map.clear();
      index = 0;
    }
  }
}
