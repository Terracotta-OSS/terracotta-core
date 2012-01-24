/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.test.TCTestCase;
import com.terracottatech.config.Client;
import com.terracottatech.config.Server;

import java.io.File;

/**
 * Unit test for {@link TestConfigurationSetupManagerFactory}. Because that class builds up a whole config system, this
 * test actually stresses a large swath of the configuration system.
 */
public class TestConfigurationSetupManagerFactoryTest extends TCTestCase {

  private TestConfigurationSetupManagerFactory factory;
  private L2ConfigurationSetupManager          l2Manager;
  private L1ConfigurationSetupManager          l1Manager;

  public TestConfigurationSetupManagerFactoryTest() {
    // this.disableAllUntil(new Date(Long.MAX_VALUE));
  }

  @Override
  public void setUp() throws Exception {
    this.factory = new TestConfigurationSetupManagerFactory(
                                                            TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                            null, new FatalIllegalConfigurationChangeHandler());

    ((Server) this.factory.l2CommonConfig().getBean()).setLogs(getTempFile("l2-logs").toString());
    ((Client) this.factory.l1CommonConfig().getBean()).setLogs(getTempFile("l1-logs").toString());

    this.l2Manager = this.factory.createL2TVSConfigurationSetupManager(null);
    this.l1Manager = this.factory.getL1TVSConfigurationSetupManager();
  }

  public void testSettingValues() throws Exception {
    // Hit the remaining top-level config objects
    this.l2Manager.dsoL2Config().garbageCollection().setInterval(142);
    ((Client) this.l1Manager.commonL1Config().getBean()).setLogs("whatever");
    ((Server) this.l2Manager.commonl2Config().getBean()).setData("marph");

    // A sub-config object
    this.l1Manager.dsoL1Config().instrumentationLoggingOptions().setLogDistributedMethods(true);

    this.factory.activateConfigurationChange();

    System.err.println(this.l2Manager.systemConfig());
    System.err.println(this.l1Manager.dsoL1Config());

    assertEquals(142, this.l2Manager.dsoL2Config().garbageCollection().getInterval());
    assertEquals(new File("whatever"), this.l1Manager.commonL1Config().logsPath());
    assertEquals(new File("marph"), this.l2Manager.commonl2Config().dataPath());
    assertTrue(this.l1Manager.dsoL1Config().instrumentationLoggingOptions().logDistributedMethods());
  }

}
