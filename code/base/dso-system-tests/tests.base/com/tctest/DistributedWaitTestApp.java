/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class DistributedWaitTestApp extends AbstractTransparentApp {

  private static final long WAIT_TIME = 15 * 1000;
  private Object            myRoot    = new Object();

  public DistributedWaitTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    synchronized (myRoot) {
      try {
        long start = System.currentTimeMillis();
        myRoot.wait(WAIT_TIME);
        long end = System.currentTimeMillis();
        Assert.assertEquals((double) WAIT_TIME, (double) (end - start), 3 * 1000.0);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = DistributedWaitTestApp.class.getName();
    ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myRoot", "myRoot");

  }
}
