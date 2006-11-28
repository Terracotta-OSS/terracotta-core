package com.tctest.spring.integrationtests.framework;

import java.io.IOException;

import junit.extensions.TestSetup;
import junit.framework.TestSuite;

public class ServerTestSetup extends TestSetup {

  private final Class testClass;
  protected ServerManager sm;

  public ServerTestSetup(Class testClass) {
    super(new TestSuite(testClass));
    this.testClass = testClass;
  }

  protected void setUp() throws Exception {
    sm = ServerManagerUtil.startAndBind(testClass, isWithPersistentStore());
  }

  protected void tearDown() throws Exception {
    ServerManagerUtil.stopAndRelease(sm);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return sm.makeDeploymentBuilder();
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return sm.makeDeploymentBuilder(warFileName);
  }

  public boolean isWithPersistentStore() {
    return false;
  }
  
}
