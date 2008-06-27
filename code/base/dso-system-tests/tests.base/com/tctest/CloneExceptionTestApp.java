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
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

/**
 * Test for DEV-865
 *
 * @author hhuynh
 */
public class CloneExceptionTestApp extends AbstractErrorCatchingTransparentApp {
  private MyStuff root = new MyStuff();

  public CloneExceptionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    synchronized (root) {
      try {
        root.clone();
      } catch (RuntimeException e) {
        System.err.println("Expecting CloneNotSupportedException, got " + e.getCause());
        Assert.assertTrue(e.getCause() instanceof CloneNotSupportedException);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CloneExceptionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(MyStuff.class.getName());
    config.addWriteAutolock("* " + MyStuff.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + testClass + "*.runTest(..)");

    spec.addRoot("root", "root");
  }

  private static class MyStuff {
    protected Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
