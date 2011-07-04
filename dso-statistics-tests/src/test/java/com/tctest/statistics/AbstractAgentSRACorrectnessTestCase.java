/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
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

import java.util.Collection;

abstract public class AbstractAgentSRACorrectnessTestCase extends BaseDSOTestCase {

  public void checkSRAsInRegistry(final StatisticsAgentSubSystem agent) {
    SRACorrectnessTest testLogic = new SRACorrectnessTest();
    StatisticsRetrievalRegistry registry = agent.getStatisticsRetrievalRegistry();
    for (StatisticRetrievalAction sra : (Collection<StatisticRetrievalAction>) registry.getRegisteredActionInstances()) {
      testLogic.checkCorrectSRA(sra);
    }
  }

  protected TCServer startupServer(final int dsoPort, final int jmxPort, int l2GroupPort) {
    StartAction start_action = new StartAction(dsoPort, jmxPort, l2GroupPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  protected DistributedObjectClient startupClient(final int dsoPort, final int jmxPort)
      throws ConfigurationSetupException {
    configFactory().addServerToL1Config("127.0.0.1", dsoPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();

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
    private final int l2groupPort;
    private TCServer  server = null;

    private StartAction(final int dsoPort, final int jmxPort, int l2groupPort) {
      this.dsoPort = dsoPort;
      this.jmxPort = jmxPort;
      this.l2groupPort = l2groupPort;
    }

    public int getDsoPort() {
      return dsoPort;
    }

    public int getJmxPort() {
      return jmxPort;
    }

    public int getL2groupPort() {
      return l2groupPort;
    }

    public TCServer getServer() {
      return server;
    }

    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestConfigurationSetupManagerFactory factory = AbstractAgentSRACorrectnessTestCase.this.configFactory();
      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);

      manager.dsoL2Config().dsoPort().setIntValue(dsoPort);
      manager.dsoL2Config().dsoPort().setBind("127.0.0.1");

      manager.commonl2Config().jmxPort().setIntValue(jmxPort);
      manager.commonl2Config().jmxPort().setBind("127.0.0.1");

      manager.dsoL2Config().l2GroupPort().setIntValue(l2groupPort);
      manager.dsoL2Config().l2GroupPort().setBind("127.0.0.1");

      server = new TCServerImpl(manager);
      server.start();
    }
  }
}
