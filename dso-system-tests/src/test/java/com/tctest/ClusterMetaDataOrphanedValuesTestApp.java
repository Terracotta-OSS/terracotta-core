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
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.ClusterMetaDataTestApp.MyMojo;
import com.tctest.ClusterMetaDataTestApp.SomePojo;
import com.tctest.ClusterMetaDataTestApp.YourMojo;
import com.tctest.builtin.HashMap;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterMetaDataOrphanedValuesTestApp extends AbstractTransparentApp {

  public static final int         NODE_COUNT  = 1;

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";

  private final ApplicationConfig config;

  public ClusterMetaDataOrphanedValuesTestApp(final String appId, final ApplicationConfig config,
                                              final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterMetaDataOrphanedValuesTestApp.class.getName();
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

    Assert.assertEquals("The test L1Client did not exit with a success status", 0, spawnNewClient());

    Thread.sleep(5000);

    Assert.assertEquals("The test L1Client did not exit with a success status", 0, spawnNewClient());
  }

  public static class L1Client {
    @InjectedDsoInstance
    private DsoCluster cluster;

    private final Map  map = new HashMap();

    public static void main(final String args[]) {
      new L1Client().runTest();
    }

    public void runTest() {
      if (0 == map.size()) {
        // creating different transactions to that active-active is able to spread the objects across servers
        synchronized (map) {
          map.put("key1", new SomePojo(new YourMojo("mojo uno"), new MyMojo("mojo dos")));
        }
        synchronized (map) {
          map.put("key2", new YourMojo("mojo tres"));
        }
        synchronized (map) {
          map.put(new MyMojo("mojo quattro"), new Object());
        }
        synchronized (map) {
          map.put(new Object(), "value4");
        }
      } else {
        synchronized (map) {
          final Set keysBeforeFault = cluster.getKeysForOrphanedValues(map);
          Assert.assertEquals("keysBeforeFault: " + keysBeforeFault, 3, keysBeforeFault.size());
          Assert.assertTrue(keysBeforeFault.contains("key1"));
          Assert.assertTrue(keysBeforeFault.contains("key2"));
          Assert.assertTrue(keysBeforeFault.contains(new MyMojo("mojo quattro")));

          map.get("key1");
          map.get("key2");

          final Set keysAfterFault = cluster.getKeysForOrphanedValues(map);
          Assert.assertEquals("keysAfterFault: " + keysAfterFault, 1, keysAfterFault.size());
          Assert.assertFalse(keysAfterFault.contains("key1"));
          Assert.assertFalse(keysAfterFault.contains("key2"));
          Assert.assertTrue(keysAfterFault.contains(new MyMojo("mojo quattro")));
        }
      }
    }
  }

  private int spawnNewClient() throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new java.util.ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class,
                                                             configFile.getAbsolutePath(), Collections.EMPTY_LIST,
                                                             workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    System.err.println("\n### Started new client - Waiting for end");
    int rv = client.waitFor();

    // Explicitly making sure that client has finished
    while (client.isRunning()) {
      ThreadUtil.reallySleep(1000);
    }

    return rv;
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
