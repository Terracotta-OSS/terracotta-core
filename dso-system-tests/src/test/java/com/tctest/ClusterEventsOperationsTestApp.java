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

public class ClusterEventsOperationsTestApp extends AbstractTransparentApp {

  public static final String              CONFIG_FILE = "config-file";
  public static final String              PORT_NUMBER = "port-number";
  public static final String              HOST_NAME   = "host-name";

  private final ApplicationConfig         config;

  @InjectedDsoInstance
  private DsoCluster                      cluster;

  private final ClusterEventsTestListener listener    = new ClusterEventsTestListener();

  public ClusterEventsOperationsTestApp(final String appId, final ApplicationConfig config,
                                        final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
    cluster.addClusterListener(listener);
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterEventsOperationsTestApp.class.getName();
    config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(testClass + "$*");

    String testEventsListenerClass = ClusterEventsTestListener.class.getName();
    config.getOrCreateSpec(testEventsListenerClass);
    config.addWriteAutolock("* " + testEventsListenerClass + "*.*(..)");

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

    Thread.sleep(10000);

    config.getServerControl().start();
    while (!config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    System.out.println("Server restarted successfully.");

    spawnNewClient();

    waitUntilOnlyOneNodeInTopology();

    // wait for all cluster events to be delivered
    Thread.sleep(5000);

    List<String> events = listener.getOccurredEvents();
    System.out.println("Occurred events");
    for (String e : events) {
      System.out.println(e);
    }
    Assert.assertEquals(6, events.size());
    Assert.assertEquals("ClientID[0] JOINED", events.get(0));
    Assert.assertEquals("ClientID[0] ENABLED", events.get(1));
    Assert.assertEquals("ClientID[0] DISABLED", events.get(2));
    Assert.assertEquals("ClientID[0] ENABLED", events.get(3));
    Assert.assertEquals("ClientID[1] JOINED", events.get(4));
    Assert.assertEquals("ClientID[1] LEFT", events.get(5));
  }

  private void waitUntilOnlyOneNodeInTopology() throws InterruptedException {
    while (cluster.getClusterTopology().getNodes().size() > 1) {
      Thread.sleep(1000);
    }
  }

  public static class L1Client {
    public static void main(final String args[]) {
      new L1Client();
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
