/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import javax.swing.Timer;

public class UnsharedClassAsRootTestApp extends AbstractTransparentApp {

  private Timer timer;

  public UnsharedClassAsRootTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    try {
      timer = new Timer(0, null);
      throw new AssertionError("Should have failed");
    }
    catch( TCNonPortableObjectError tnp) {
        // Expected
    }
  }

  public void run() {
    Assert.assertNull(timer);
    System.err.println("Timer = " + timer);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = UnsharedClassAsRootTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("timer", "timer");
  }

}
