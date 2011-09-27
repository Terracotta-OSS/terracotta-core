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
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterMetaDataPrefetchTestApp extends AbstractTransparentApp {

  public static final int         NODE_COUNT  = 1;

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";

  private final ApplicationConfig config;

  public ClusterMetaDataPrefetchTestApp(final String appId, final ApplicationConfig config,
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

    Thread.sleep(10000);

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
          Set keysForOrphanedValues = cluster.getKeysForOrphanedValues(map);
          Set keysForLocalValues = cluster.getKeysForLocalValues(map);
          Assert.assertEquals("orphaned keys are " + keysForOrphanedValues, 2, keysForOrphanedValues.size());
          Assert.assertEquals("local keys are " + keysForLocalValues, 2, keysForLocalValues.size());

          for (Object value : map.values()) {
            value.toString();
          }

          keysForOrphanedValues = cluster.getKeysForOrphanedValues(map);
          keysForLocalValues = cluster.getKeysForLocalValues(map);
          Assert.assertEquals("orphaned keys are " + keysForOrphanedValues, 0, keysForOrphanedValues.size());
          Assert.assertEquals("local keys are " + keysForLocalValues, 4, keysForLocalValues.size());
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

    List jvmArgs = new ArrayList();
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
