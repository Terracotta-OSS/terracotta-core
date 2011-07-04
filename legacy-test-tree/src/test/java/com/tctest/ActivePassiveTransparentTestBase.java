/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.test.DSOApplicationConfigBuilderImpl;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.PortChooser;
import com.terracottatech.config.TcConfigDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ActivePassiveTransparentTestBase extends MultipleServersTransparentTestBase {

  @Override
  protected void runMultipleServersTest() throws Exception {
    customizeActivePassiveTest((ActivePassiveServerManager) multipleServerManager);
  }

  protected abstract void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager);

  @Override
  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    setUpActivePassiveServers(portChooser, jvmArgs);
  }

  // to be override for L1 application config
  protected DSOApplicationConfigBuilder createDsoApplicationConfig() {
    return (new DSOApplicationConfigBuilderImpl());
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
                                                                                canRunL2ProxyConnect(),
                                                                                canRunL1ProxyConnect(),
                                                                                createDsoApplicationConfig());

    setupServersAndGroupsForL1s(apServerManager);
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(apServerManager.getL2ProxyManagers());
    if (canRunL1ProxyConnect()) setupL1ProxyConnectTest(apServerManager.getL1ProxyManagers());

    multipleServerManager = apServerManager;
  }

  private void setupServersAndGroupsForL1s(ActivePassiveServerManager apServerManager) throws XmlException,
      IOException, ConfigurationSetupException {
    File configFile = new File(apServerManager.getConfigFileLocation());
    TcConfigDocument configDoc = TcConfigDocument.Factory.parse(configFile);
    L2DSOConfigObject.initializeServers(configDoc.getTcConfig(), new SchemaDefaultValueProvider(), configFile
        .getParentFile());
    apServerManager.addServersAndGroupToL1Config(configFactory(), configDoc.getTcConfig().getServers());
  }

  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    manager.startActivePassiveServers();
  }

  @Override
  public boolean isMultipleServerTest() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE.equals(mode());
  }

}
