/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClusterEventsTestApp extends AbstractTransparentApp {

  public static final int              NODE_COUNT  = 1;

  public static final String           CONFIG_FILE = "config-file";
  public static final String           PORT_NUMBER = "port-number";
  public static final String           HOST_NAME   = "host-name";

  private final ApplicationConfig      config;

  @InjectedDsoInstance
  private DsoCluster                   cluster;

  private final ClusterEventsTestState state       = new ClusterEventsTestState();

  public ClusterEventsTestApp(final String appId, final ApplicationConfig config,
                              final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
    cluster.addClusterListener(state.getListenerForNode(cluster.getCurrentNode()));
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testMainClass = ClusterEventsTestApp.class.getName();
    config.getOrCreateSpec(testMainClass);
    config.addWriteAutolock("* " + testMainClass + "*.*(..)");

    String testEventsL1Client = ClusterEventsL1Client.class.getName();
    config.getOrCreateSpec(testEventsL1Client);

    String testEventsListenerClass = ClusterEventsTestListener.class.getName();
    config.getOrCreateSpec(testEventsListenerClass);
    config.addWriteAutolock("* " + testEventsListenerClass + "*.*(..)");

    String testStateClass = ClusterEventsTestState.class.getName();
    config.getOrCreateSpec(testStateClass).addRoot("listeners", "listeners");
    config.addWriteAutolock("* " + testStateClass + "*.*(..)");
  }

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    Thread.sleep(5000);

    spawnNewClient(1);

    waitUntilOnlyOneNodeInTopology();

    spawnNewClient(2);

    waitUntilOnlyOneNodeInTopology();

    Assert.assertEquals(3, state.getListeners().size());

    // wait for all cluster events to be delivered
    Thread.sleep(5000);

    List<String> node0Events = state.getListeners().get("ClientID[0]").getOccurredEvents();
    System.out.println("Events at ClientID[0]");
    for (String e : node0Events) {
      System.out.println(e);
    }
    Assert.assertEquals(6, node0Events.size());
    Assert.assertEquals("ClientID[0] JOINED", node0Events.get(0));
    Assert.assertEquals("ClientID[0] ENABLED", node0Events.get(1));
    Assert.assertEquals("ClientID[1] JOINED", node0Events.get(2));
    Assert.assertEquals("ClientID[1] LEFT", node0Events.get(3));
    Assert.assertEquals("ClientID[2] JOINED", node0Events.get(4));
    Assert.assertEquals("ClientID[2] LEFT", node0Events.get(5));

    List<String> node1Events = state.getListeners().get("ClientID[1]").getOccurredEvents();
    System.out.println("Events at ClientID[1]");
    for (String e : node1Events) {
      System.out.println(e);
    }
    Assert.assertEquals(2, node1Events.size());
    Assert.assertEquals("ClientID[1] JOINED", node1Events.get(0));
    Assert.assertEquals("ClientID[1] ENABLED", node1Events.get(1));

    List<String> node2Events = state.getListeners().get("ClientID[2]").getOccurredEvents();
    System.out.println("Events at ClientID[2]");
    for (String e : node2Events) {
      System.out.println(e);
    }
    Assert.assertEquals(2, node2Events.size());
    Assert.assertEquals("ClientID[2] JOINED", node2Events.get(0));
    Assert.assertEquals("ClientID[2] ENABLED", node2Events.get(1));
  }

  private void waitUntilOnlyOneNodeInTopology() throws InterruptedException {
    while (cluster.getClusterTopology().getNodes().size() > 1) {
      Thread.sleep(1000);
    }
  }

  private ExtraL1ProcessControl spawnNewClient(final int id) throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-" + id);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, ClusterEventsL1Client.class,
                                                             configFile.getAbsolutePath(), Collections.EMPTY_LIST,
                                                             workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

  private void addTestTcPropertiesFile(final List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) { return; }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) { return; }
    System.err.println("\n##### -Dcom.tc.properties=" + pathToTestTcProperties);
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
  }

}
