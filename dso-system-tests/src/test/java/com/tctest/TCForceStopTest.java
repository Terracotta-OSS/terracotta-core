/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.TCStop;
import com.tc.object.BaseDSOTestCase;
import com.tc.server.util.ServerStat;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class TCForceStopTest extends BaseDSOTestCase {
  private static final String SERVER_NAME_2      = "server-2";
  private static final String SERVER_NAME_1      = "server-1";
  private TcConfigBuilder     configBuilder;
  private ExternalDsoServer   server_1, server_2;
  private int                 managementPort_1, managementPort_2;
  private final long          SHUTDOWN_WAIT_TIME = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/tc-force-stop-test.xml");
    configBuilder.randomizePorts();

    managementPort_1 = configBuilder.getManagementPort(0);
    managementPort_2 = configBuilder.getManagementPort(1);

    server_1 = createServer(SERVER_NAME_1);
    server_2 = createServer(SERVER_NAME_2);
    server_1.addJvmArg("-Dcom.tc.l2.enable.legacy.production.mode=true");
    server_2.addJvmArg("-Dcom.tc.l2.enable.legacy.production.mode=true");
    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(managementPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(managementPort_2);
    System.out.println("server2 became passive");

  }

  public void testServerForceStop() throws Exception {
    // Case : 1 Active + 1 Passive
    stop(managementPort_1, false);

    Assert.assertFalse(server_1.isRunning());
    waitTillBecomeActive(managementPort_2);
    // Case : Only 1 active server
    stop(managementPort_2, false);
    Assert.assertTrue(server_2.isRunning());
    // Case : only 1 active server force shutdown
    stop(managementPort_2, true);
    Assert.assertFalse(server_2.isRunning());
  }

  private void stop(int jmxPort, boolean force) {
    try {
      TCStop.restStop("localhost", jmxPort, null, null, force, false, false);
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

  private boolean isActive(int managementPort) {
    try {
      ServerStat stats = ServerStat.getStats("localhost", managementPort, null, null, false, true);
      return "ACTIVE-COORDINATOR".equals(stats.getState());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isPassiveStandBy(int managementPort) {
    try {
      ServerStat stats = ServerStat.getStats("localhost", managementPort, null, null, false, true);
      return "PASSIVE-STANDBY".equals(stats.getState());
    } catch (Exception e) {
      return false;
    }
  }

  private void waitTillBecomeActive(int managementPort) {
    while (true) {
      if (isActive(managementPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int managementPort) {
    while (true) {
      if (isPassiveStandBy(managementPort)) break;
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
