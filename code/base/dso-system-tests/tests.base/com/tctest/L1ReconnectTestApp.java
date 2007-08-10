/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.tc.net.proxy.TCPProxy;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for l1-reconnect feature: CDV-97 An extra L1 is created and connected to L2 through a proxy (TCPProxy) We will
 * use this proxy to simulate network disconnect to make sure the L1 is still operating correctly upon reconnecting
 * within a specified time NOT YET FINISHED
 * 
 * @author hhuynh
 */
public class L1ReconnectTestApp extends AbstractTransparentApp {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";

  private final ApplicationConfig config;

  // roots
  private int[]                   sum         = new int[1];

  public L1ReconnectTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = L1ReconnectTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    spec.addRoot("sum", "sum");
  }

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    PortChooser pc = new PortChooser();
    int dsoPort = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    int dsoProxyPort = pc.chooseRandomPort();

    // new TCPProxy(int listenPort, InetAddress destHost, int destPort, long delay, boolean logData, File logDir
    TCPProxy proxy = new TCPProxy(dsoProxyPort, InetAddress.getLocalHost(), dsoPort, 0L, false, new File("."));
    proxy.setReuseAddress(true);
    proxy.start();

    ExtraL1ProcessControl client = spawnNewClient(dsoProxyPort);
    // this L1 client will take approximately 100s to finish

    boolean stopProxy = false;
    while (!stopProxy) {
      synchronized (sum) {
        if (sum[0] > 10) stopProxy = true;
      }
    }
    System.err.println("\n\n### stopping proxy...");
    proxy.stop();
    System.err.println("\n\n### stopping proxy...done");

    ThreadUtil.reallySleep(1 * 1000);
    System.err.println("\n\n### starting proxy...");
    proxy.start();
    System.err.println("\n\n### starting proxy...done");

    // here we want to simulate network glitches by
    // turn off the proxy and turn it back on

    int exitCode = client.waitFor();
    proxy.status();
    proxy.stop();

    if (exitCode != 0) {
      Assert.failure("L1Client threw exception!");
    }

    synchronized (sum) {
      System.out.println("SUM = " + sum[0]);
      Assert.assertEquals(99, sum[0]);
    }
  }

  public static class L1Client {
    // roots
    private int[] sum = new int[1];

    // takes roughly 100 seconds to finish
    public void calculateSum() throws Exception {
      for (int i = 0; i < 100; i++) {
        if (i > 0 && (i % 10) == 0) ThreadUtil.reallySleep(10 * 100);
        synchronized (sum) {
          sum[0] = i;
        }
        System.err.println("\n\n### Transaction # " + i);
      }
    }

    public static void main(String args[]) {
      try {
        new L1Client().calculateSum();
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  private ExtraL1ProcessControl spawnNewClient(int dsoProxyPort) throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    final File proxyConfigFile = createNewConfigFile(configFile, dsoProxyPort);
    File workingDir = new File(configFile.getParentFile(), "l1client");
    FileUtils.forceMkdir(workingDir);

    ArrayList jvmArgs = new ArrayList();
    jvmArgs.add("-Dcom.tc.l1.reconnect.enabled=true");

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, 0 /* not used */, L1Client.class,
                                                             proxyConfigFile.getAbsolutePath(), new String[0],
                                                             workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    System.err.println("\n### Started New Client");
    return client;
  }

  private File createNewConfigFile(File configFile, int dsoProxyPort) throws Exception {
    List lines = IOUtils.readLines(new FileInputStream(configFile));
    for (int i = 0; i < lines.size(); i++) {
      String line = (String) lines.get(i);
      if (line.indexOf("dso-port") > 0) {
        String newLine = line.replaceAll("\\d+", dsoProxyPort + "");
        lines.set(i, newLine);
        break;
      }
    }

    File newConfigFile = new File(configFile.getParent(), "tc-config-proxy.xml");
    IOUtils.writeLines(lines, "\n", new FileOutputStream(newConfigFile));

    return newConfigFile;
  }

}
