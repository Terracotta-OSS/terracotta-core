/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.server.TCServerImpl;
import com.tc.simulator.listener.MockListenerProvider;
import com.tcsimulator.SimpleApplicationConfig;

public class DSOApplicationBuilderTest extends BaseDSOTestCase {

  private DSOApplicationBuilder   builder;
  private SimpleApplicationConfig applicationConfig;
  private TCServerImpl server;

  public void setUp() throws Exception {
    TestTVSConfigurationSetupManagerFactory factory = super.configFactory();
    L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);

    // minor hack to make server listen on an OS assigned port
//    ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(0);

    server = new TCServerImpl(manager);
    server.start();

    DSOClientConfigHelper configHelper = configHelper();

    configHelper.addExcludePattern(SimpleApplicationConfig.class.getName());

    makeClientUsePort(server.getDSOListenPort());

    this.applicationConfig = new SimpleApplicationConfig();

    L1TVSConfigurationSetupManager configManager = factory.createL1TVSConfigurationSetupManager();
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(configManager);

    this.builder = new DSOApplicationBuilder(configHelper, this.applicationConfig, components);
  }

  public void testNewApplication() throws Exception {
    Application application = this.builder.newApplication("test", new MockListenerProvider());
    assertEquals(this.applicationConfig.getApplicationClassname(), application.getClass().getName());
  }

}
