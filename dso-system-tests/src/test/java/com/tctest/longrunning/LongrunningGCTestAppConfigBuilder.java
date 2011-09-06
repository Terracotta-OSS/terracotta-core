/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ApplicationConfigBuilder;

public class LongrunningGCTestAppConfigBuilder implements ApplicationConfigBuilder {

  private ConfigVisitor visitor;

  public LongrunningGCTestAppConfigBuilder() {
    this.visitor = new ConfigVisitor();
  }

  public void visitClassLoaderConfig(DSOClientConfigHelper config) {
    this.visitor.visit(config, LongrunningGCTestApp.class);
  }

  public ApplicationConfig newApplicationConfig() {
    LongrunningGCTestAppConfigObject rv = new LongrunningGCTestAppConfigObject();
    try {
      long sleepTime = Long.parseLong(System.getProperty("com.tctest.longrunning.LongrunningGCTestApp.loopSleepTime"));
      System.err.println("Setting loop sleep time to: " + sleepTime);
      rv.setLoopSleepTime(sleepTime);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return rv;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper cfg) {
    visitor.visit(cfg, LongrunningGCTestApp.class);
  }

}