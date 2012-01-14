/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.xmlbeans.XmlException;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.server.TCServerImpl;
import com.tc.test.JMXUtils;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class GroupInfoFromHttpSystemTest extends BaseDSOTestCase {

  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int               jmxPort_1, jmxPort_2;
  private int               dsoPort_1, dsoPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/active-passive-fail-over-test.xml");
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    dsoPort_1 = configBuilder.getDsoPort(0);
    dsoPort_2 = configBuilder.getDsoPort(1);

    server_1 = createServer("server-1");
    server_2 = createServer("server-2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

  }

  public void testGetGroupInfo() throws Exception {
    testGroupInfoForServer(dsoPort_1, true);
    testGroupInfoForServer(dsoPort_2, false);
    server_1.stop();
    waitTillBecomeActive(jmxPort_2);
    testGroupInfoForServer(dsoPort_1, false);
    testGroupInfoForServer(dsoPort_2, true);
  }

  private void testGroupInfoForServer(int dsoPort, boolean shouldPass) throws MalformedURLException {
    URL theURL = new URL("http", "localhost", dsoPort, TCServerImpl.GROUP_INFO_SERVLET_PATH);
    InputStream l1PropFromL2Stream = null;
    System.out.println("Trying to get groupinfo from " + theURL.toString());
    int trials = 0;
    while (true) {
      try {
        URLConnection connection = theURL.openConnection();
        l1PropFromL2Stream = connection.getInputStream();
        Assert.assertNotNull(l1PropFromL2Stream);
        break;
      } catch (IOException e) {
        if (shouldPass) {
          System.out.println("should have connected to [localhost:" + dsoPort + "]. trials so far: " + ++trials);
        } else {
          break;
        }
      }
    }

    if (shouldPass) {
      try {
        ServerGroupsDocument.Factory.parse(l1PropFromL2Stream);
      } catch (XmlException e) {
        if (shouldPass) {
          Assert.fail("should have been able to parse the groupinfo");
        }
      } catch (IOException e) {
        if (shouldPass) {
          Assert.fail("should have been able to parse the groupinfo");
        }
      }
    }
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

  private boolean isPassiveStandBy(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isPassiveStandBy = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isPassiveStandBy = mbean.isPassiveStandby();
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

    return isPassiveStandBy;
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int jmxPort) {
    while (true) {
      if (isPassiveStandBy(jmxPort)) break;
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
    if (server_1 != null && server_1.isRunning()) server_1.stop();
    if (server_2 != null && server_2.isRunning()) server_2.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

}
