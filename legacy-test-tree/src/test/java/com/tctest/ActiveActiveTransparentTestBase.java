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
import com.tc.test.activeactive.ActiveActiveServerManager;
import com.tc.test.activeactive.ActiveActiveTestSetupManager;
import com.tc.util.PortChooser;
import com.terracottatech.config.TcConfigDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ActiveActiveTransparentTestBase extends MultipleServersTransparentTestBase {

  @Override
  protected void runMultipleServersTest() throws Exception {
    customizeActiveActiveTest((ActiveActiveServerManager) multipleServerManager);
  }

  protected void customizeActiveActiveTest(ActiveActiveServerManager manager) throws Exception {
    manager.startActiveActiveServers();
  }

  @Override
  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    setUpActiveActiveServers(portChooser, jvmArgs);
  }

  // to be override for L1 application config
  protected DSOApplicationConfigBuilder createDsoApplicationConfig() {
    return (new DSOApplicationConfigBuilderImpl());
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
                                                                              canRunL2ProxyConnect(),
                                                                              canRunL1ProxyConnect(),
                                                                              createDsoApplicationConfig());
    setupServersAndGroupsForL1s(aaServerManager);
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(aaServerManager.getL2ProxyManagers());
    if (canRunL1ProxyConnect()) setupL1ProxyConnectTest(aaServerManager.getL1ProxyManagers());

    multipleServerManager = aaServerManager;
  }

  private void setupServersAndGroupsForL1s(ActiveActiveServerManager aaServerManager) throws XmlException, IOException,
      ConfigurationSetupException {
    File configFile = new File(aaServerManager.getConfigFileLocation());
    TcConfigDocument configDoc = TcConfigDocument.Factory.parse(configFile);
    L2DSOConfigObject.initializeServers(configDoc.getTcConfig(), new SchemaDefaultValueProvider(), configFile
        .getParentFile());
    aaServerManager.addGroupsToL1Config(configFactory(), configDoc.getTcConfig().getServers());
  }

  @Override
  public boolean isMultipleServerTest() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE.equals(mode());
  }

  protected abstract void setupActiveActiveTest(ActiveActiveTestSetupManager setupManager);

}
