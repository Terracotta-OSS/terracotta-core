/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.BaseDSOTestCase;
import com.tc.server.util.ServerStat;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;

public class TCStopTest extends BaseDSOTestCase {
  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int managementPort_1, managementPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/active-passive-fail-over-test.xml");
    configBuilder.randomizePorts();

    managementPort_1 = configBuilder.getManagementPort(0);
    managementPort_2 = configBuilder.getManagementPort(1);

    server_1 = createServer("server-1");
    server_2 = createServer("server-2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(managementPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(managementPort_2);
    System.out.println("server2 became passive");

  }

  public void testServerStop() throws Exception {
    server_1.stop();
    server_2.stop();
    Assert.assertFalse(server_1.isRunning());
    Assert.assertFalse(server_2.isRunning());
  }

  private boolean isActive(int managementPort) {
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", managementPort, null, null, false, false);
      return "ACTIVE-COORDINATOR".equals(serverStat.getState());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isPassiveStandBy(int managementPort) {
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", managementPort, null, null, false, false);
      return "PASSIVE-STANDBY".equals(serverStat.getState());
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
