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
import com.tcclient.cluster.DsoNode;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClusterMetaDataAfterNodeLeftTestApp extends AbstractTransparentApp {

  public static final int              NODE_COUNT  = 1;

  public static final String           CONFIG_FILE = "config-file";
  public static final String           PORT_NUMBER = "port-number";
  public static final String           HOST_NAME   = "host-name";

  private final ApplicationConfig      config;

  @InjectedDsoInstance
  private DsoCluster                   cluster;

  private final ClusterEventsTestState state       = new ClusterEventsTestState();

  public ClusterMetaDataAfterNodeLeftTestApp(final String appId, final ApplicationConfig config,
                                             final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
    cluster.addClusterListener(state.getListenerForNode(cluster.getCurrentNode()));
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testMainClass = ClusterMetaDataAfterNodeLeftTestApp.class.getName();
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

    for (ClusterEventsTestListener listener : state.getListeners().values()) {
      for (DsoNode node : listener.getEventNodes()) {
        Assert.assertNotNull(node.getIp());
        Assert.assertNotNull(node.getHostname());
      }
    }

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
                                                             configFile.getAbsolutePath(), Arrays.asList(String
                                                                 .valueOf(id)), workingDir, jvmArgs);
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
