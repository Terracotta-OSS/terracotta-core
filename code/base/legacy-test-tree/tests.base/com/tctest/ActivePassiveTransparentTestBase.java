/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.List;

public abstract class ActivePassiveTransparentTestBase extends MultipleServersTransparentTestBase {

  protected void runMultipleServersTest() throws Exception {
    customizeActivePassiveTest((ActivePassiveServerManager) multipleServerManager);
  }

  protected abstract void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager);

  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    setUpActivePassiveServers(portChooser, jvmArgs);
  }

  private void setUpActivePassiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    ActivePassiveTestSetupManager apSetupManager = new ActivePassiveTestSetupManager();
    setupActivePassiveTest(apSetupManager);
    ActivePassiveServerManager apServerManager = new ActivePassiveServerManager(mode()
        .equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE), getTempDirectory(), portChooser,
                                                                                MultipleServersConfigCreator.DEV_MODE,
                                                                                apSetupManager, javaHome,
                                                                                configFactory(), jvmArgs,
                                                                                canRunL2ProxyConnect());
    apServerManager.addServersAndGroupToL1Config(configFactory());
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(apServerManager.getL2ProxyManagers());

    multipleServerManager = apServerManager;
  }

  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    manager.startActivePassiveServers();
  }

}
