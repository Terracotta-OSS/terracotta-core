package com.tc.test.jmx;

import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.PauseManager;
import org.terracotta.tests.base.TestClientManager;

import com.tc.test.config.model.TestConfig;
import com.tc.test.setup.GroupsData;
import com.tc.test.setup.TestServerManager;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class TestHandler implements TestHandlerMBean {
  public static final ObjectName         TEST_SERVER_CONTROL_MBEAN;

  static {
    try {
      TEST_SERVER_CONTROL_MBEAN = new ObjectName("com.tc.test.jmx:type=TestHandler");
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    } catch (NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  private final TestServerManager        testServerManager;
  private final TestClientManager        testClientManager;
  private final TestConfig               testConfig;
  private final PauseManager             pauseManager;
  private volatile CustomCommandExecutor executor;

  public TestHandler(TestServerManager manager, final TestClientManager testClientManager, TestConfig testConfig,
                     PauseManager pauseManager) {
    this.testServerManager = manager;
    this.testClientManager = testClientManager;
    this.testConfig = testConfig;
    this.pauseManager = pauseManager;
  }

  public int getNumberOfGroups() {
    return testServerManager.getNumberOfGroups();
  }

  @Override
  public File getTempDir() {
    return testClientManager.getTempDir();
  }

  @Override
  public void crashActiveServer(int groupIndex) throws Exception {
    testServerManager.crashActiveServer(groupIndex);
  }

  @Override
  public void crashActiveAndWaitForPassiveToTakeOver(int groupIndex) throws Exception {
    testServerManager.crashActiveAndWaitForPassiveToTakeOver(groupIndex);
  }

  @Override
  public void restartCrashedServer(int groupIndex, int serverIndex) throws Exception {
    testServerManager.restartCrashedServer(groupIndex, serverIndex);
  }

  @Override
  public void restartLastCrashedServer(final int groupIndex) throws Exception {
    testServerManager.restartLastCrashedServer(groupIndex);
  }

  @Override
  public void dumpClusterState() throws Exception {
    testServerManager.dumpClusterState(1, 0);
  }

  @Override
  public void crashAllPassiveServers(int groupIndex) throws Exception {
    testServerManager.crashAllPassive(groupIndex);
  }

  @Override
  public void crashPassive(int groupIndex, int serverIndex) throws Exception {
    testServerManager.crashPassive(groupIndex, serverIndex);
  }

  @Override
  public boolean isActivePresent(int groupIndex) throws Exception {
    return testServerManager.isActivePresent(groupIndex);
  }

  @Override
  public boolean isPassiveStandBy(int groupIndex) throws Exception {
    return testServerManager.isPassiveStandBy(groupIndex);
  }

  @Override
  public boolean isPassiveUninitialized(final int groupIndex, final int serverIndex) {
    return testServerManager.isPassiveUninitialized(groupIndex, serverIndex);
  }

  @Override
  public void waitUntilPassiveStandBy(int groupIndex) throws Exception {
    testServerManager.waitUntilPassiveStandBy(groupIndex);
  }

  @Override
  public void waitUntilEveryPassiveStandBy(int groupIndex) throws Exception {
    testServerManager.waitUntilEveryPassiveStandBy(groupIndex);
  }

  @Override
  public void waitUntilActive(final int groupIndex) throws Exception {
    testServerManager.waitUntilActive(groupIndex);
  }

  @Override
  public GroupsData[] getGroupsData() {
    return testServerManager.getGroupsData();
  }

  @Override
  public String getTerracottaUrl() {
    return TestBaseUtil.getTerracottaURL(getGroupsData(), false);
  }

  @Override
  public int getParticipantCount() {
    return this.testConfig.getClientConfig().getClientClasses().length;
  }

  @Override
  public void startServer(int groupIndex, int serverIndex) throws Exception {
    testServerManager.startServer(groupIndex, serverIndex);
  }

  @Override
  public boolean isStandAloneTest() {
    return this.testConfig.isStandAloneTest();
  }

  @Override
  public void clientExitedWithException(Throwable t) {
    t.printStackTrace();
    testClientManager.clientExitedWithException(t);
  }

  @Override
  public boolean isServerRunning(int groupIndex, int serverIndex) {
    return testServerManager.isServerRunning(groupIndex, serverIndex);
  }

  @Override
  public int waitForServerExit(final int groupIndex, final int serverIndex) throws Exception {
    return testServerManager.waitForServerExit(groupIndex, serverIndex);
  }

  @Override
  public Serializable executeCustomCommand(String cmd, Serializable[] params) {
    if (executor != null) { return executor.execute(cmd, params); }
    return "No Command executor registered. Failed to execute: " + cmd + ", params: " + Arrays.asList(params);
  }

  @Override
  public void stopTsaProxy(int groupIndex) throws Exception {
    testServerManager.stopTsaProxy(groupIndex);
  }

  @Override
  public void closeTsaProxyConnections(final int groupIndex) throws Exception {
    testServerManager.closeTsaProxy(groupIndex);
  }

  @Override
  public void startTsaProxy(int groupIndex) throws Exception {
    testServerManager.startTsaProxy(groupIndex);
  }

  public void setCustomCommandExecutor(CustomCommandExecutor executor) {
    this.executor = executor;
  }

  @Override
  public String getTsaProxyTerracottaUrl() {
    return TestBaseUtil.getTerracottaURL(getGroupsData(), true);
  }

  @Override
  public String getTsaProxyTcConfig() throws Exception {
    return testServerManager.getTsaProxyConfig();
  }

  @Override
  public String getTsaProxyTcConfigFilePath() {
    return testServerManager.getTsaProxyConfigFile().getAbsolutePath();
  }

  @Override
  public void runClient(final Class<? extends Runnable> client, final String clientName, final List<String> extraMainClassArgs) throws Throwable {
    testClientManager.runClient(client, clientName, extraMainClassArgs);
  }

  @Override
  public void startCrasher() {
    testServerManager.startServerCrasher();

  }

  @Override
  public void pauseClient(int clientIndex) throws Exception {
    pauseManager.pauseClient(clientIndex);
  }

  @Override
  public void unpauseClient(int clientIndex) throws Exception {
    pauseManager.unpauseClient(clientIndex);
  }

  @Override
  public void pauseServer(int groupIndex, int serverIndex) throws Exception {
    pauseManager.pauseServer(groupIndex, serverIndex);
  }

  @Override
  public void unpauseServer(int groupIndex, int serverIndex) throws Exception {
    pauseManager.unpauseServer(groupIndex, serverIndex);
  }

  @Override
  public int getActiveServerIndex(int groupIndex) {
    return testServerManager.getActiveServerIndex(groupIndex);
  }

}
