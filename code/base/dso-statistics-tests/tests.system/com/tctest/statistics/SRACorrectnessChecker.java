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
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.PauseListener;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.NullTCServerInfo;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.TCAssertionError;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public class SRACorrectnessChecker extends BaseDSOTestCase {

  public void testCorrectnessLogic() throws Exception {
    try {
      checkCorrectSRA(new BadSRAMissingName());
      fail();
    } catch (NullPointerException e) {
      assertEquals("name is null", e.getMessage());
    }

    try {
      checkCorrectSRA(new BadSRAMissingType());
      fail();
    } catch (NullPointerException e) {
      assertEquals("type is null", e.getMessage());
    }

    try {
      checkCorrectSRA(new BadSRANonFinalField());
      fail();
    } catch (TCAssertionError e) {
      // expected
    }

    checkCorrectSRA(new CorrectSRA());
  }

  public void testCheckL2SRAs() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final DistributedObjectServer server = startupServer(dsoPort, jmxPort);

    try {
      // verify that all the registered SRA classes are correct
      StatisticsAgentSubSystem agent = server.getStatisticsAgentSubSystem();
      checkSRAsInRegistry(agent);
    } finally {
      server.stop();
    }
    Thread.sleep(3000);
  }

  public void testCheckL1SRAs() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final DistributedObjectServer server = startupServer(dsoPort, jmxPort);

    try {
      final TestPauseListener pauseListener = new TestPauseListener();
      final DistributedObjectClient client = startupClient(dsoPort, jmxPort, pauseListener);
      try {
        // wait until client handshake is complete...
        pauseListener.waitUntilUnpaused();

        // verify that all the registered SRA classes are correctSRAStageQueueDepthsTest
        StatisticsAgentSubSystem agent = client.getStatisticsAgentSubSystem();
        checkSRAsInRegistry(agent);
      } finally {
        client.getCommunicationsManager().shutdown();
        Thread.sleep(3000);
      }
    } finally {
      server.stop();
    }
    Thread.sleep(3000);
  }

  private void checkSRAsInRegistry(final StatisticsAgentSubSystem agent) {
    StatisticsRetrievalRegistry registry = agent.getStatisticsRetrievalRegistry();
    for (StatisticRetrievalAction sra : (Collection<StatisticRetrievalAction>)registry.getRegisteredActionInstances()) {
      checkCorrectSRA(sra);
    }
  }

  private void checkCorrectSRA(final StatisticRetrievalAction sra) {
    System.out.println("Checking " + sra);
    Assert.assertNotNull("name", sra.getName());
    Assert.assertNotNull("type", sra.getType());
    Class sraClass = sra.getClass();
    Field[] fields = sraClass.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isFinal(field.getModifiers())) {
        Assert.fail("The '" + field.getName() + "' of SRA class '" + sraClass.getName() + "' is not final.");
      }
    }
  }

  private DistributedObjectServer startupServer(final int dsoPort, final int jmxPort) {
    StartAction start_action = new StartAction(dsoPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final DistributedObjectServer server = start_action.getServer();
    return server;
  }

  private DistributedObjectClient startupClient(final int dsoPort, final int jmxPort, final TestPauseListener pauseListener) throws ConfigurationSetupException {
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
    client.setCreateDedicatedMBeanServer(true);
    client.start();
    return client;
  }

  static class BadSRAMissingName implements StatisticRetrievalAction {
    public StatisticData[] retrieveStatisticData() { return StatisticRetrievalAction.EMPTY_STATISTIC_DATA; }
    public String getName() { return null; }
    public StatisticType getType() { return StatisticType.SNAPSHOT; }
  }

  static class BadSRAMissingType implements StatisticRetrievalAction {
    public StatisticData[] retrieveStatisticData() { return StatisticRetrievalAction.EMPTY_STATISTIC_DATA; }
    public String getName() { return "name"; }
    public StatisticType getType() { return null; }
  }

  static class BadSRANonFinalField implements StatisticRetrievalAction {
    private String nonFinalField = "test";
    public StatisticData[] retrieveStatisticData() { return StatisticRetrievalAction.EMPTY_STATISTIC_DATA; }
    public String getName() { return "name"; }
    public StatisticType getType() { return StatisticType.STARTUP; }
  }

  static class CorrectSRA implements StatisticRetrievalAction {
    private final String finalField = "test";
    public StatisticData[] retrieveStatisticData() { return StatisticRetrievalAction.EMPTY_STATISTIC_DATA; }
    public String getName() { return "name"; }
    public StatisticType getType() { return StatisticType.STARTUP; }
  }

  private static final class TestPauseListener implements PauseListener {

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

  private final TCThreadGroup  group = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(DistributedObjectServer.class)));

  private class StartAction implements StartupHelper.StartupAction {
    private final int dsoPort;
    private final int jmxPort;
    private DistributedObjectServer server = null;

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

    public DistributedObjectServer getServer() {
      return server;
    }

    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      server = new DistributedObjectServer(createL2Manager("127.0.0.1"), group, new NullConnectionPolicy(), new NullTCServerInfo());
      server.start();
    }

    private L2TVSConfigurationSetupManager createL2Manager(String bindAddress) throws ConfigurationSetupException {
      TestTVSConfigurationSetupManagerFactory factory = SRACorrectnessChecker.this.configFactory();
      L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
      ((SettableConfigItem) factory.l2DSOConfig().bind()).setValue(bindAddress);
      ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) factory.l2CommonConfig().jmxPort()).setValue(jmxPort);
      return manager;
    }
  }
}