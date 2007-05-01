/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;


import java.io.IOException;
import java.util.Enumeration;

import junit.extensions.TestSetup;
import junit.framework.TestSuite;

public class ServerTestSetup extends TestSetup {

  private final Class testClass;

  private ServerManager sm;

  public ServerTestSetup(Class testClass) {
    super(new TestSuite(testClass));
    this.testClass = testClass;
  }

  protected void tearDown() throws Exception {
    if (sm != null) {
      ServerManagerUtil.stopAndRelease(sm);
    }
  }
  
  protected ServerManager getServerManager() {
    if (sm == null) {
      try {
        sm = ServerManagerUtil.startAndBind(testClass, isWithPersistentStore());
      } catch (Exception e) {
        throw new RuntimeException("Unable to create server manager", e);
      }
    }
    return sm;
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return getServerManager().makeDeploymentBuilder();
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return getServerManager().makeDeploymentBuilder(warFileName);
  }

  public boolean isWithPersistentStore() {
    return false;
  }
  
  public boolean shouldDisable() {
    for (Enumeration e = ((TestSuite) fTest).tests(); e.hasMoreElements();) {
      Object o = e.nextElement();
      if (o instanceof AbstractDeploymentTest && ((AbstractDeploymentTest) o).shouldDisable()) {
        return true;
      }
    }
    return false;
  }
  
}
