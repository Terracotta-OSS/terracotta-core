/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.l2.state.StateManager;
import com.tc.object.BaseDSOTestCase;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.server.util.ServerStat;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class OssOperatorEventLogsSystemTest extends BaseDSOTestCase {
  private final String      serverName = "server-1";
  private TcConfigBuilder   configBuilder;
  private ExternalDsoServer server_1;
  private int               managementPort_1;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tctest/operator-event-logs-system-test.xml");
    configBuilder.randomizePorts();
    managementPort_1 = configBuilder.getManagementPort(0);
    server_1 = createServer(serverName);
  }

  public void testDatabaseState() throws Exception {
    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(managementPort_1);
    System.out.println("server1 became active");

    TerracottaOperatorEvent movedToActiveOpEvent = TerracottaOperatorEventFactory
        .createClusterNodeStateChangedEvent(StateManager.ACTIVE_COORDINATOR.getName());

    File serverLog = new File(server_1.getWorkingDir(), serverName + "-logs/terracotta-server.log");
    System.out.println("server log location " + serverLog.getAbsolutePath());
    FileInputStream fstream = new FileInputStream(serverLog);
    DataInputStream in = new DataInputStream(fstream);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String strLine;
    while ((strLine = br.readLine()) != null) {
      if (strLine.contains("tc.operator.event") && strLine.contains(movedToActiveOpEvent.getEventMessage())) {
        br.close();
        in.close();
        return;
      }
    }
    // Close the input stream
    in.close();
    Assert.fail("Operator event was not logged");
  }

  private boolean isActive(int managementPort) {
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", managementPort, null, null, false, false);
      return "ACTIVE-COORDINATOR".equals(serverStat.getState());
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

  private ExternalDsoServer createServer(final String serverName1) throws IOException {
    ExternalDsoServer server = new ExternalDsoServer(getWorkDir(serverName1), configBuilder.newInputStream(),
                                                     serverName);
    return server;
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (server_1 != null) server_1.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

}
