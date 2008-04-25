/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import junit.extensions.TestSetup;
import junit.framework.TestSuite;

public class CoresidentServerTestSetup extends TestSetup {

  private final Class testClass;
  private final Collection extraJvmArgs;
  private final boolean persistentMode;
  private ServerManager[] managers;

  public CoresidentServerTestSetup(Class testClass) {
    this(testClass, false, Collections.EMPTY_LIST);
  }

  public CoresidentServerTestSetup(Class testClass, boolean persistentMode, Collection extraJvmArgs) {
    super(new TestSuite(testClass));
    this.testClass = testClass;
    this.persistentMode = persistentMode;
    this.extraJvmArgs = extraJvmArgs;
  }

  protected void setUp() throws Exception {
    super.setUp();
    getServerManagers();
  }

  protected void tearDown() throws Exception {
    if (managers != null) {
      for (int i = 0; i < managers.length; i++) {
        ServerManager manager = managers[i];
        ServerManagerUtil.stopAndRelease(manager, true);
      }
    }
  }

  public ServerManager[] getServerManagers() {
    if (managers == null) {
        try {
          managers = new ServerManager[2];
          managers[0] = ServerManagerUtil.startAndBind(testClass, isWithPersistentStore(), extraJvmArgs, true);
          managers[1] = ServerManagerUtil.startAndBind(testClass, isWithPersistentStore(), extraJvmArgs, true);
        } catch (Exception e) {
          throw new RuntimeException("Unable to create server manager", e);
        }
    }
    return managers;
  }

  public AppServerInfo appServerInfo() {
    return TestConfigObject.getInstance().appServerInfo();
  }

  public DeploymentBuilder makeDeploymentBuilder(ServerManager serverManager) throws IOException {
    return serverManager.makeDeploymentBuilder();
  }

  public DeploymentBuilder makeDeploymentBuilder(ServerManager serverManager, String warFileName) {
    return serverManager.makeDeploymentBuilder(warFileName);
  }

  public boolean isWithPersistentStore() {
    return persistentMode;
  }

  public boolean shouldDisable() {
    for (Enumeration e = ((TestSuite)fTest).tests(); e.hasMoreElements();) {
      Object o = e.nextElement();
      if (o instanceof AbstractDeploymentTest && ((AbstractDeploymentTest)o).shouldDisable()) { return true; }
    }
    return false;
  }
}