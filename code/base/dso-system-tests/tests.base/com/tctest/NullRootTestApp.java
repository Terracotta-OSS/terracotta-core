/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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

public class NullRootTestApp extends AbstractTransparentApp {

  private Map root = null;
  private static Map staticRoot = null;

  public NullRootTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    if (root == null) {
      root = new HashMap();
    } else {
      root = null;
    }

    if (staticRoot == null) {
      staticRoot = new HashMap();
    } else {
      staticRoot = null;
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NullRootTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    spec.addRoot("staticRoot", "staticRoot");
  }

}
