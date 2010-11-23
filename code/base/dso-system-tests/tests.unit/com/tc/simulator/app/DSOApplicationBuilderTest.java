/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.properties.TCPropertiesConsts;
import com.tc.server.TCServerImpl;
import com.tc.simulator.container.IsolationClassLoaderFactory;
import com.tc.simulator.listener.MockListenerProvider;
import com.tcsimulator.SimpleApplication;
import com.tcsimulator.SimpleApplicationConfig;

public class DSOApplicationBuilderTest extends BaseDSOTestCase {

  private DSOApplicationBuilder   builder;
  private SimpleApplicationConfig applicationConfig;
  private TCServerImpl            server;

  @Override
  public void setUp() throws Exception {
    TestConfigurationSetupManagerFactory factory = super.configFactory();
    L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);

    // minor hack to make server listen on an OS assigned port
    server = new TCServerImpl(manager);
    server.start();

    factory.addServerToL1Config("localhost", server.getDSOListenPort(), 0);
    factory.addTcPropertyToConfig(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "false");

    this.applicationConfig = new SimpleApplicationConfig();

    L1ConfigurationSetupManager configManager = factory.getL1TVSConfigurationSetupManager();

    this.builder = new DSOApplicationBuilder(new IsolationClassLoaderFactory(this, SimpleApplication.class, null,
        configManager, null) {
      @Override
      protected DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException {
        DSOClientConfigHelper result = super.createClientConfigHelper();
        result.addExcludePattern(SimpleApplicationConfig.class.getName());
        return result;
      }
    }, this.applicationConfig);
  }

  public void testNewApplication() throws Exception {
    Application application = this.builder.newApplication("test", new MockListenerProvider());
    assertEquals(this.applicationConfig.getApplicationClassname(), application.getClass().getName());
  }

}
