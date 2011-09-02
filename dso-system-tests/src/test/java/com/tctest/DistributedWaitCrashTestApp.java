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
import com.tctest.runner.AbstractTransparentApp;

public class DistributedWaitCrashTestApp extends AbstractTransparentApp {

  private static final long WAIT_TIME = 15 * 1000;
  private Object            myRoot    = new Object();

  public DistributedWaitCrashTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    int wokeupCount = 0;
    for (int i = 0; i < 4; i++) {
      synchronized (myRoot) {
        try {
          myRoot.wait(WAIT_TIME);
          System.out.println("woke up count: " + wokeupCount++);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
    Assert.assertEquals(4, wokeupCount);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = DistributedWaitCrashTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myRoot", "myRoot");

  }
}
