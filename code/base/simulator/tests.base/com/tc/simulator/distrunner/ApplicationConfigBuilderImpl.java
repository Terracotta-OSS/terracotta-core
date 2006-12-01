/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tcsimulator.ApplicationConfigImpl;

public class ApplicationConfigBuilderImpl implements ApplicationConfigBuilder {

  private final String applicatonClassname;
  private final int intensity;
  private final int globalParticipantCount;

  public ApplicationConfigBuilderImpl(String applicatonClassname, int intensity, int globalPariticipantCount) {
    this.applicatonClassname = applicatonClassname;
    this.intensity = intensity;
    this.globalParticipantCount = globalPariticipantCount;
  }

  public void visitClassLoaderConfig(DSOClientConfigHelper config) {
    System.err.println("visitClassLoaderConfig called: " + config);
  }

  public ApplicationConfig newApplicationConfig() {
    //new RuntimeException("Creating new ApplicationConfigImpl()...").printStackTrace();
    return new ApplicationConfigImpl(this.applicatonClassname, this.intensity, this.globalParticipantCount);
  }
}
