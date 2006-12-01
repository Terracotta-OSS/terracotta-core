/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.server.NullTCServerInfo;
import com.tc.simulator.listener.MockListenerProvider;
import com.tcsimulator.SimpleApplicationConfig;

public class DSOApplicationBuilderTest extends BaseDSOTestCase {

  private DSOApplicationBuilder   builder;
  private SimpleApplicationConfig applicationConfig;
  private DistributedObjectServer server;

  public void setUp() throws Exception {
    TestTVSConfigurationSetupManagerFactory factory = super.configFactory();
    L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);

    // minor hack to make server listen on an OS assigned port
    ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(0);

    server = new DistributedObjectServer(manager, new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectServer.class))), new NullConnectionPolicy(), new NullTCServerInfo());
    server.start();

    DSOClientConfigHelper configHelper = configHelper();

    configHelper.addExcludePattern(SimpleApplicationConfig.class.getName());

    makeClientUsePort(server.getListenPort());

    this.applicationConfig = new SimpleApplicationConfig();

    L1TVSConfigurationSetupManager configManager = factory.createL1TVSConfigurationSetupManager();
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(configManager);

    this.builder = new DSOApplicationBuilder(configHelper, this.applicationConfig, components);
  }

  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  public void testNewApplication() throws Exception {
    Application application = this.builder.newApplication("test", new MockListenerProvider());
    assertEquals(this.applicationConfig.getApplicationClassname(), application.getClass().getName());
  }

}
