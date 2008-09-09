/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.activeactive.ActiveActiveServerManager;
import com.tc.test.activeactive.ActiveActiveTestSetupManager;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.List;

public abstract class ActiveActiveTransparentTestBase extends MultipleServersTransparentTestBase {

  protected void runMultipleServersTest() throws Exception {
    customizeActiveActiveTest((ActiveActiveServerManager) multipleServerManager);
  }

  protected void customizeActiveActiveTest(ActiveActiveServerManager manager) throws Exception {
    manager.startActiveActiveServers();
  }

  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    setUpActiveActiveServers(portChooser, jvmArgs);
  }

  private void setUpActiveActiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    ActiveActiveTestSetupManager aaSetupManager = new ActiveActiveTestSetupManager();
    setupActiveActiveTest(aaSetupManager);
    ActiveActiveServerManager aaServerManager = new ActiveActiveServerManager(getTempDirectory(), portChooser,
                                                                              MultipleServersConfigCreator.DEV_MODE,
                                                                              aaSetupManager, javaHome,
                                                                              configFactory(), jvmArgs,
                                                                              canRunL2ProxyConnect());
    aaServerManager.addGroupsToL1Config(configFactory());
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(aaServerManager.getL2ProxyManagers());

    multipleServerManager = aaServerManager;
  }

  protected abstract void setupActiveActiveTest(ActiveActiveTestSetupManager setupManager);

}
