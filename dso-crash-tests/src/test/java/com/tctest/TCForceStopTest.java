/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.TCStop;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.test.JMXUtils;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class TCForceStopTest extends BaseDSOTestCase {
  private static final String SERVER_NAME_2      = "server-2";
  private static final String SERVER_NAME_1      = "server-1";
  private TcConfigBuilder     configBuilder;
  private ExternalDsoServer   server_1, server_2;
  private int                 jmxPort_1, jmxPort_2;
  private final long          SHUTDOWN_WAIT_TIME = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/tc-force-stop-test.xml");
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer(SERVER_NAME_1);
    server_2 = createServer(SERVER_NAME_2);

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

  }

  public void testServerForceStop() throws Exception {
    // Case : 1 Active + 1 Passive
    stop(server_1, SERVER_NAME_1);

    Assert.assertFalse(server_1.isRunning());
    waitTillBecomeActive(jmxPort_2);
    // Case : Only 1 active server
    stop(server_2, SERVER_NAME_2);
    Assert.assertTrue(server_2.isRunning());
    // Case : only 1 active server force shutdown
    forceStop(server_2, SERVER_NAME_2);
    Assert.assertFalse(server_2.isRunning());

  }

  private void stop(ExternalDsoServer server, String serverName) {
    System.out.println("Going to stop server :" + server.getAdminPort());
    stop(server.getAdminPort(),
         getCommandLineArgsForStop(serverName, server.getConfigFile().getPath(), server.getAdminPort()));
  }

  private void forceStop(ExternalDsoServer server, String serverName) {
    System.out.println("Going to force stop server :" + server.getAdminPort());
    stop(server.getAdminPort(),
         getCommandLineArgsForForceStop(serverName, server.getConfigFile().getPath(), server.getAdminPort()));
  }

  private String[] getCommandLineArgsForStop(String serverName, String configFilePath, int port) {
    return new String[] { StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, configFilePath,
        StandardConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName, Integer.toString(port), };

  }

  private String[] getCommandLineArgsForForceStop(String serverName, String configFilePath, int port) {
    return new String[] { StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, configFilePath,
        StandardConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName, Integer.toString(port),
        "-force" };
  }

  private void stop(int jmxPort, String[] args) {
    try {
      TCStop.main(args);
    } catch (Exception e) {
      System.out.println("Exception while stopping server :" + jmxPort);
      System.out.println(e);
    }
    try {
      waitUntilShutdown(jmxPort);
    } catch (Exception e) {
      System.out.println("Exception while stopping server :" + jmxPort);
      System.out.println(e);
    }
  }

  private void waitUntilShutdown(int jmxPort) throws Exception {
    long start = System.nanoTime();
    long timeout = start + SHUTDOWN_WAIT_TIME;
    while (isRunning(jmxPort)) {
      Thread.sleep(1000);
      if (System.nanoTime() > timeout) {
        System.out.println("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME);
        break;
      }
    }
  }

  private boolean isRunning(int jmxPort) {
    Socket socket = null;
    try {
      socket = new Socket("localhost", jmxPort);
      if (!socket.isConnected()) throw new AssertionError();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ioe) {
          // ignore
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
