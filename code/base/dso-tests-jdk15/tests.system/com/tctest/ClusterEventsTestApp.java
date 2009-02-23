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
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClusterEventsTestApp extends AbstractTransparentApp {

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

    String testEventsL1Client = "com.tctest.ClusterEventsL1Client";
    config.getOrCreateSpec(testEventsL1Client);

    String testEventsListenerClass = "com.tctest.ClusterEventsTestListener";
    config.getOrCreateSpec(testEventsListenerClass);
    config.addWriteAutolock("* " + testEventsListenerClass + "*.*(..)");

    String testStateClass = "com.tctest.ClusterEventsTestState";
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
/*
    config.getServerControl().crash();
    while (config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }

    config.getServerControl().start();
    while (!config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
*/
    spawnNewClient(1);

    spawnNewClient(2);

    while (cluster.getClusterTopology().getNodes().size() > 1) {
      Thread.sleep(1000);
    }

    for (Map.Entry<String, ClusterEventsTestListener> entry : state.getListeners().entrySet()) {
      System.out.println(entry.getKey());
      System.out.println(entry.getValue());
      for (String event : entry.getValue().getOccurredEvents()) {
        System.out.println(event);
      }
    }
  }

  private ExtraL1ProcessControl spawnNewClient(final int id) throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-"+id);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, ClusterEventsL1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir, jvmArgs);
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
