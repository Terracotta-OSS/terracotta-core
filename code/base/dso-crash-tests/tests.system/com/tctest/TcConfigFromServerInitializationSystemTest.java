/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class TcConfigFromServerInitializationSystemTest extends BaseDSOTestCase {
  private TcConfigBuilder   configBuilder;
  private ExternalDsoServer dsoServer;
  private int               adminPort, dsoPort;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/tc-config-from-server-initializatio-test.xml");
    configBuilder.randomizePorts();

    dsoPort = configBuilder.getDsoPort();
    adminPort = configBuilder.getJmxPort();

    dsoServer = createServer("server-1");

    dsoServer.start();
    System.out.println("server1 started");
    waitTillBecomeActive(adminPort);
    System.out.println("server1 became active");
  }

  public void testFailover() throws Exception {
    ExtraL1ProcessControl client1 = createClient(0, 2);
    client1.start();
    ThreadUtil.reallySleep(30000);

    ExtraL1ProcessControl client2 = createClient(0, 2);
    client2.start();
    ThreadUtil.reallySleep(30000);
    File workingDirectory = new File(TcConfigFromServerInitializationSystemTest.class.getSimpleName() + File.separator
                                     + "l1client0");
    System.out.println("XXXX " + workingDirectory.getAbsolutePath());
    Assert.assertTrue(workingDirectory.exists());
    Assert.assertTrue(workingDirectory.isDirectory());
    String[] extensions = { "log" };
    Collection logFiles = FileUtils.listFiles(workingDirectory, extensions, true);
    Assert.assertEquals(2, logFiles.size());
  }

  private boolean isActive(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(null, null, "localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private ExternalDsoServer createServer(final String serverName) throws IOException {
    ExternalDsoServer server = new ExternalDsoServer(getWorkDir(serverName), configBuilder.newInputStream(), serverName);
    return server;
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (dsoServer != null) dsoServer.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

  private ExtraL1ProcessControl createClient(int clientIndex, int numOfClients) throws IOException {
    List jvmArgs = new ArrayList();
    jvmArgs.add("-Dtc.node-name=node" + clientIndex);
    jvmArgs.add("-Dtc.config=localhost:" + dsoPort);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl("localhost", dsoPort, L1.class, "localhost:" + dsoPort,
                                                             Arrays.asList("" + clientIndex, "" + numOfClients),
                                                             getWorkDir("l1client" + clientIndex), jvmArgs);
    return client;
  }

  public static class L1 {
    public static void main(final String[] args) {
      System.out.println(System.getProperty("l1.name") + ": started");
      ThreadUtil.reallySleep(10 * 60 * 1000);
      System.out.println(System.getProperty("l1.name") + ": stopped");
    }
  }
}
