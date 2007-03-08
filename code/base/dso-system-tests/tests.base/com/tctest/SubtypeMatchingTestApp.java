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
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.transparency.MatchingSubclass1;
import com.tctest.transparency.MatchingSubclass2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Kuleshov
 */
public class SubtypeMatchingTestApp extends AbstractTransparentApp {

  private List list = new ArrayList();

  public SubtypeMatchingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      synchronized (list) {
        list.add(new MatchingSubclass1());
        list.add(new MatchingSubclass2());
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("list", "list");
    
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    config.addIncludePattern("com.tctest.transparency.MarkerInterface+");
    config.addIncludePattern("com.tctest.transparency.MatchingClass+");
  }

}
