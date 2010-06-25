/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.terracottatech.config.BindPort;

import java.util.Collection;

abstract public class AbstractAgentSRACorrectnessTestCase extends BaseDSOTestCase {

  public void checkSRAsInRegistry(final StatisticsAgentSubSystem agent) {
    SRACorrectnessTest testLogic = new SRACorrectnessTest();
    StatisticsRetrievalRegistry registry = agent.getStatisticsRetrievalRegistry();
    for (StatisticRetrievalAction sra : (Collection<StatisticRetrievalAction>) registry.getRegisteredActionInstances()) {
      testLogic.checkCorrectSRA(sra);
    }
  }

  protected TCServer startupServer(final int dsoPort, final int jmxPort) {
    StartAction start_action = new StartAction(dsoPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  protected DistributedObjectClient startupClient(final int dsoPort, final int jmxPort)
      throws ConfigurationSetupException {
    configFactory().addServerToL1Config(null, dsoPort, jmxPort);
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();

    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(),
                                                                 new PreparedComponentsFromL2Connection(manager),
                                                                 NullManager.getInstance(),
                                                                 new StatisticsAgentSubSystemImpl(),
                                                                 new DsoClusterImpl(), new NullRuntimeLogger());
    client.start();
    return client;
  }

  protected final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(TCLogging
                                          .getLogger(DistributedObjectServer.class)));

  protected class StartAction implements StartupHelper.StartupAction {
    private final int dsoPort;
    private final int jmxPort;
    private TCServer  server = null;

    private StartAction(final int dsoPort, final int jmxPort) {
      this.dsoPort = dsoPort;
      this.jmxPort = jmxPort;
    }

    public int getDsoPort() {
      return dsoPort;
    }

    public int getJmxPort() {
      return jmxPort;
    }

    public TCServer getServer() {
      return server;
    }

    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestTVSConfigurationSetupManagerFactory factory = AbstractAgentSRACorrectnessTestCase.this.configFactory();
      L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
      ((SettableConfigItem) factory.l2DSOConfig().bind()).setValue("127.0.0.1");

      BindPort dsoBindPort = BindPort.Factory.newInstance();
      dsoBindPort.setIntValue(dsoPort);
      dsoBindPort.setBind("127.0.0.1");
      ((SettableConfigItem) factory.l2DSOConfig().dsoPort()).setValue(dsoBindPort);

      BindPort jmxBindPort = BindPort.Factory.newInstance();
      jmxBindPort.setIntValue(jmxPort);
      jmxBindPort.setBind("127.0.0.1");
      ((SettableConfigItem) factory.l2CommonConfig().jmxPort()).setValue(jmxBindPort);

      server = new TCServerImpl(manager);
      server.start();
    }
  }
}
