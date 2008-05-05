/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class DEV1588Test extends TransparentTestBase {

  private final static int NODE_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final Map root = new HashMap();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      shareObject();
      System.gc();
      synchronized (root) {
        root.put("bite", "me");
      }
    }

    private void shareObject() {
      // This is a separate method to make sure the local Object can be GC'd
      ManagerUtil.shareObjectIfNecessary(new Object());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      spec.addRoot("root", "root");

      config.addWriteAutolock("* " + testClass + ".*(..)");

    }

  }
}
