/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.test.JMXUtils;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class OverlappingDataDirectoryDiskBasedTest extends BaseDSOTestCase {

  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int               jmxPort_1, jmxPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  public void testFailover() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/overlapping-data-directory-disk-based-test.xml");
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server1");
    server_2 = createServer("server2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    server_1.stop();
    waitTillBecomeActive(jmxPort_2);
    System.out.println("server2 became active");
    System.out.println("test passed...");
  }

  private boolean isActive(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
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
    if (server_1 != null) server_1.stop();
    if (server_2 != null) server_2.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

}
