/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.cluster.Cluster;
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
import com.tc.object.PauseListener;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

import java.util.Collection;

abstract public class AbstractAgentSRACorrectnessTestCase extends BaseDSOTestCase {

  public void checkSRAsInRegistry(final StatisticsAgentSubSystem agent) {
    SRACorrectnessTest testLogic = new SRACorrectnessTest();
    StatisticsRetrievalRegistry registry = agent.getStatisticsRetrievalRegistry();
    for (StatisticRetrievalAction sra : (Collection<StatisticRetrievalAction>)registry.getRegisteredActionInstances()) {
      testLogic.checkCorrectSRA(sra);
    }
  }

  protected TCServer startupServer(final int dsoPort, final int jmxPort) {
    StartAction start_action = new StartAction(dsoPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  protected DistributedObjectClient startupClient(final int dsoPort, final int jmxPort, final TestPauseListener pauseListener) throws ConfigurationSetupException {
    configFactory().addServerToL1Config(null, dsoPort, jmxPort);
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();

    DistributedObjectClient client = new DistributedObjectClient(
      new StandardDSOClientConfigHelperImpl(manager),
      new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class))),
      new MockClassProvider(),
      new PreparedComponentsFromL2Connection(manager),
      NullManager.getInstance(),
      new Cluster());
    client.setPauseListener(pauseListener);
    client.start();
    return client;
  }

  protected final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(DistributedObjectServer.class)));

  protected class StartAction implements StartupHelper.StartupAction {
    private final int dsoPort;
    private final int jmxPort;
    private TCServer server = null;

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
      ((SettableConfigItem)factory.l2DSOConfig().bind()).setValue("127.0.0.1");
      ((SettableConfigItem)factory.l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem)factory.l2CommonConfig().jmxPort()).setValue(jmxPort);

      server = new TCServerImpl(manager);
      server.start();
    }
  }

  protected static final class TestPauseListener implements PauseListener {

    private boolean paused = true;

    public void waitUntilPaused() throws InterruptedException {
      waitUntilCondition(true);
    }

    public void waitUntilUnpaused() throws InterruptedException {
      waitUntilCondition(false);
    }

    public boolean isPaused() {
      synchronized (this) {
        return paused;
      }
    }

    private void waitUntilCondition(boolean b) throws InterruptedException {
      synchronized (this) {
        while (b != paused) {
          wait();
        }
      }
    }

    public void notifyPause() {
      synchronized (this) {
        paused = true;
        notifyAll();
      }
    }

    public void notifyUnpause() {
      synchronized (this) {
        paused = false;
        notifyAll();
      }
    }
  }
}