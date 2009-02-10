/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
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

public class ClusterEventsTestApp extends AbstractTransparentApp implements DsoClusterListener {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";

  private final ApplicationConfig config;

  @InjectedDsoInstance
  private DsoCluster              cluster;

  public ClusterEventsTestApp(final String appId, final ApplicationConfig config,
                              final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
    cluster.addClusterListener(this);
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterEventsTestApp.class.getName();
    config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(testClass + "$*");
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

    config.getServerControl().crash();
    while (config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    // this sleep should be longer than l1-reconnect timeout
    Thread.sleep(30 * 1000);
    config.getServerControl().start();
    while (!config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    System.out.println("Server restarted successfully.");
    spawnNewClient();
  }

  public void nodeJoined(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeJoined : " + event.getNode().getId());
  }

  public void nodeLeft(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeLeft : " + event.getNode().getId());
  }

  public void operationsDisabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsDisabled : " + event.getNode().getId());
  }

  public void operationsEnabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsEnabled : " + event.getNode().getId());
  }

  public static class L1Client implements DsoClusterListener {
    @InjectedDsoInstance
    private DsoCluster              cluster;

    public static void main(final String args[]) {
      new L1Client();
    }

    public L1Client() {
      cluster.addClusterListener(this);
    }

    public void nodeJoined(final DsoClusterEvent event) {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeJoined : " + event.getNode().getId());
    }

    public void nodeLeft(final DsoClusterEvent event) {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeLeft : " + event.getNode().getId());
    }

    public void operationsDisabled(final DsoClusterEvent event) {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsDisabled : " + event.getNode().getId());
    }

    public void operationsEnabled(final DsoClusterEvent event) {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsEnabled : " + event.getNode().getId());
    }
  }

  private ExtraL1ProcessControl spawnNewClient() throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class,
                                                             configFile.getAbsolutePath(), new String[0],
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
