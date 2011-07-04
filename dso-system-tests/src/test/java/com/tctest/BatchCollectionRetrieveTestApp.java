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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BatchCollectionRetrieveTestApp extends AbstractTransparentApp {
  private TestRoot  root;
  private final Set nodes = new HashSet();

  public BatchCollectionRetrieveTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {

    synchronized (nodes) {

      this.root = new TestRoot();
      if (nodes.size() == 0) {
        root.setBigMap(createBigHashMap());
      } else {
        long l = System.currentTimeMillis();
        root.getBigMap().toString();
        System.out.println("******Took******:" + (System.currentTimeMillis() - l));
      }
      nodes.add(new Object());
    }
  }

  private Map createBigHashMap() {
    Map m = new HashMap();
    for (int i = 0; i < 1000; i++) {
      m.put(Integer.valueOf(i), new HashMap());
    }
    return m;
  }

  private static class TestRoot {
    private Map bigMap;

    public void setBigMap(Map m) {
      this.bigMap = m;
    }

    public Map getBigMap() {
      return this.bigMap;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = BatchCollectionRetrieveTestApp.class.getName();
    config.getOrCreateSpec(TestRoot.class.getName());
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("nodes", "nodes");
  }
}