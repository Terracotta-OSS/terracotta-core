package com.tc.test.setup;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.terracotta.license.util.IOUtils;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.tests.base.TestFailureListener;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.config.test.schema.PortConfigBuilder;
import com.tc.config.test.schema.PortConfigBuilder.PortType;
import com.tc.stats.api.DGCMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.config.model.TestConfig;
import com.tc.text.Banner;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

public class TestServerManager {
  private final TestConfig           testConfig;
  private final PortChooser          portChooser;
  private final GroupServerManager[] groups;
  private final ConfigHelper         configHelper;
  private static final boolean       DEBUG = Boolean.getBoolean("test.framework.debug");
  private final File                 tcConfigFile;

  private org.eclipse.jetty.server.Server server = null;
  private int proxyJettyPort;

  public TestServerManager(TestConfig testConfig, File tempDir, File tcConfigFile, File javaHome,
                           TestFailureListener failureCallback) throws Exception {
    this.testConfig = testConfig;
    portChooser = new PortChooser();

    this.configHelper = new ConfigHelper(portChooser, testConfig, tcConfigFile, tempDir);
    this.configHelper.writeConfigFile();
    final int numOfGroups = testConfig.getNumOfGroups();
    this.groups = new GroupServerManager[numOfGroups];

    for (int groupIndex = 0; groupIndex < this.groups.length; groupIndex++) {
      this.groups[groupIndex] = new GroupServerManager(configHelper.getGroupData(groupIndex), testConfig, tempDir,
                                                       javaHome, tcConfigFile, failureCallback);
    }
    this.tcConfigFile = tcConfigFile;
  }

  public void startAllServers() throws Exception {
    int grpCount = testConfig.getNumOfGroups();
    Thread[] threads = new Thread[grpCount];
    for (int i = 0; i < grpCount; i++) {
      final GroupServerManager serverManager = groups[i];

      Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            serverManager.startAllServers();
            if (testConfig.getCrashConfig().autoStartCrasher()) {
              serverManager.startCrasher();
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };

      threads[i] = new Thread(r);
    }

    for (int i = 0; i < grpCount; i++) {
      threads[i].start();
    }

    for (int i = 0; i < grpCount; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public void startServer(final int groupIndex, final int serverIndex) throws Exception {
    Assert.assertTrue("groupIndex" + groupIndex + " no. of groups: " + groups.length, groupIndex < groups.length);
    Assert.assertTrue("serverIndex" + serverIndex + " no. of servers per Group: "
                      + groups[groupIndex].getGroupData().getServerCount(), serverIndex < groups[groupIndex]
        .getGroupData().getServerCount());

    groups[groupIndex].startServer(serverIndex);

  }

  public void startServerNoWait(final int groupIndex, final int serverIndex) throws Exception {
    Assert.assertTrue("groupIndex" + groupIndex + " no. of groups: " + groups.length, groupIndex < groups.length);
    Assert.assertTrue("serverIndex" + serverIndex + " no. of servers per Group: "
                      + groups[groupIndex].getGroupData().getServerCount(), serverIndex < groups[groupIndex]
        .getGroupData().getServerCount());

    groups[groupIndex].startServerNoWait(serverIndex);
  }

  public void stopAllServers() throws Exception {
    debugPrintln("***** stopping all servers");
    int grpCount = testConfig.getNumOfGroups();
    Thread[] threads = new Thread[grpCount];
    for (int i = 0; i < grpCount; i++) {
      final GroupServerManager serverManager = groups[i];

      threads[i] = new Thread() {
        @Override
        public void run() {
          try {
            serverManager.stop();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };

    }

    for (int i = 0; i < grpCount; i++) {
      threads[i].start();
    }

    for (int i = 0; i < grpCount; i++) {
      threads[i].join();
    }

    synchronized (this) {
      if (server != null) {
        server.stop();
      }
    }
  }

  public void crashActiveServer(int groupIndex) throws Exception {
    groups[groupIndex].crashActive();
  }

  public void crashActiveAndWaitForPassiveToTakeOver(int groupIndex) throws Exception {
    groups[groupIndex].crashActiveAndWaitForPassiveToTakeOver();
  }

  public void restartLastCrashedServer(int groupIndex) throws Exception {
    Assert.assertTrue("groupIndex" + groupIndex + " no. of groups: " + groups.length, groupIndex < groups.length);
    groups[groupIndex].restartLastCrashedServer();
  }

  public void restartCrashedServer(int groupIndex, int serverIndex) throws Exception {
    Assert.assertTrue("groupIndex" + groupIndex + " no. of groups: " + groups.length, groupIndex < groups.length);
    Assert.assertTrue("serverIndex" + serverIndex + " no. of servers per Group: "
                      + groups[groupIndex].getGroupData().getServerCount(), serverIndex < groups[groupIndex]
        .getGroupData().getServerCount());

    groups[groupIndex].restartCrashedServer(serverIndex);
  }

  public void crashRandomServer(int groupIndex) throws Exception {
    groups[groupIndex].crashRandomServer();
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public void dumpClusterState(int dumpCount, long dumpInterval) {
    for (int i = 0; i < dumpCount; i++) {
      dumpClusterState();
      ThreadUtil.reallySleep(dumpInterval);
    }
  }

  public void dumpClusterState() {
    debugPrintln("***** dumping ClusterState ");
    int grpCount = testConfig.getNumOfGroups();
    for (int i = 0; i < grpCount; i++) {
      try {
        System.out.println("Trying to take dump for " + i + "th group");
        groups[i].dumpClusterState();
      } catch (Exception e) {
        System.out.println("Error while taking cluster dump for " + i + "th group,printing stack trace");
        e.printStackTrace();
      }
    }
  }

  public GroupsData getGroupData(int groupIndex) {
    return groups[groupIndex].getGroupData();
  }

  public void crashAllPassive(int groupIndex) throws Exception {
    groups[groupIndex].crashAllPassive();
  }

  public void crashPassive(int groupIndex, int serverIndex) throws Exception {
    groups[groupIndex].crashPassive(serverIndex);
  }

  public void crashServer(int groupIndex, int serverIndex) throws Exception {
    groups[groupIndex].crashServer(serverIndex);
  }

  public boolean isActivePresent(int groupIndex) {
    return groups[groupIndex].isActivePresent();
  }

  public int getActiveServerIndex(int groupIndex) {
    return groups[groupIndex].getActiveServerIndex();
  }

  public boolean isPassiveStandBy(int groupIndex) {
    return groups[groupIndex].isPassiveStandBy();
  }

  public boolean isServerRunning(int groupIndex, int serverIndex) {
    return groups[groupIndex].isServerRunning(serverIndex);
  }

  public boolean isPassiveUninitialized(int groupIndex, int serverIndex) {
    return groups[groupIndex].isPassiveUninitialized(serverIndex);
  }

  public void waitUntilPassiveStandBy(int groupIndex) throws Exception {
    groups[groupIndex].waituntilPassiveStandBy();
  }

  public void waitUntilActive(int groupIndex) throws Exception {
    groups[groupIndex].waitUntilActive();
  }

  public void waitUntilEveryPassiveStandBy(int groupIndex) throws Exception {
    groups[groupIndex].waituntilEveryPassiveStandBy();
  }

  public int getNumberOfGroups() {
    return testConfig.getNumOfGroups();
  }

  public GroupsData[] getGroupsData() {
    GroupsData[] groupsData = new GroupsData[groups.length];
    for (int i = 0; i < groups.length; i++) {
      groupsData[i] = groups[i].getGroupData();
    }
    return groupsData;
  }

  public List<DGCMBean> getAllLocalDGCMbeans() {
    List<DGCMBean> dgcMbeans = new ArrayList<DGCMBean>();
    for (GroupServerManager groupServerManager : groups) {
      dgcMbeans.addAll(groupServerManager.connectAllLocalDGCMBeans());
    }
    return dgcMbeans;
  }

  public List<DSOMBean> getAllDSOMbeans() {
    List<DSOMBean> dsoMbeans = new ArrayList<DSOMBean>();
    for (GroupServerManager groupServerManager : groups) {
      dsoMbeans.addAll(groupServerManager.connectAllDsoMBeans());
    }
    return dsoMbeans;
  }

  /**
   * This will close the connections between the servers of the group and the client
   * 
   * @param groupIndex the group index for which the client connections is to be closed
   */
  public void closeTsaProxy(int groupIndex) {
    Assert.assertTrue(groupIndex >= 0 && groupIndex < groups.length);
    this.groups[groupIndex].closeTsaProxyOnActiveServer();
  }

  public void stopTsaProxy(int groupIndex) {
    Assert.assertTrue(groupIndex >= 0 && groupIndex < groups.length);
    this.groups[groupIndex].stopTsaProxyOnActiveServer();
  }

  public void startTsaProxy(int groupIndex) {
    Assert.assertTrue(groupIndex >= 0 && groupIndex < groups.length);
    this.groups[groupIndex].startTsaProxyOnActiveServer();
  }

  public int waitForServerExit(int groupIndex, int serverIndex) throws Exception {
    return groups[groupIndex].waitForServerExit(serverIndex);
  }

  // writes tc-config-proxy.xml with proxy ports
  public String getTsaProxyConfig() throws Exception {
    String tcConfig = IOUtils.readToString(new FileInputStream(tcConfigFile));
    if (testConfig.getL2Config().isProxyTsaPorts()) {
      for (GroupsData groupData : getGroupsData()) {
        for (int i = 0; i < testConfig.getGroupConfig().getMemberCount(); i++) {
          PortConfigBuilder tsaPortConfig = new PortConfigBuilder(PortType.TSAPORT);
          tsaPortConfig.setBindPort(groupData.getTsaPort(i));
          PortConfigBuilder proxyTsaPortConfig = new PortConfigBuilder(PortType.TSAPORT);
          proxyTsaPortConfig.setBindPort(groupData.getProxyTsaPort(i));
          tcConfig = tcConfig.replace(tsaPortConfig.toString(), proxyTsaPortConfig.toString());
        }
      }
    }
    return tcConfig;
  }

  public File getTsaProxyConfigFile() {
    if (testConfig.getL2Config().isProxyTsaPorts()) {
      File file = new File(tcConfigFile.getParent(), AbstractTestBase.TC_CONFIG_PROXY_FILE_NAME);
      if (file.exists()) { return file; }
    }
    return null;
  }

  public void startServerCrasher() {
    for (GroupServerManager groupServerManager : groups) {
      groupServerManager.startCrasher();
    }

  }

  public void pauseServer(int groupIndex, int serverIndex, long pauseTimeMillis) throws InterruptedException {
    groups[groupIndex].pauseServer(serverIndex, pauseTimeMillis); // need to see how this will work out
  }

  public void pauseServer(int groupIndex, int serverIndex) throws InterruptedException {
    groups[groupIndex].pauseServer(serverIndex);
  }

  public void unpauseServer(int groupIndex, int serverIndex) throws InterruptedException {
    groups[groupIndex].unpauseServer(serverIndex);
  }

  public synchronized String getTerracottaUrl() throws Exception {
    // If we're not proxying, just return the plain old URL
    if (!testConfig.getL2Config().isProxyTsaPorts()) {
      return TestBaseUtil.getTerracottaURL(getGroupsData(), false);
    }

    // If we're proxying, make sure our jetty server for serving up the proxy tc-config is started then return
    // the url to the jetty server
    if (server == null) {
      proxyJettyPort = portChooser.chooseRandomPort();
      server = new org.eclipse.jetty.server.Server(proxyJettyPort);
      server.setHandler(new ProxyConfigHandler());
      server.start();
      Banner.infoBanner("Started Jetty server for proxied tc-config on port " + proxyJettyPort);
    }
    return "localhost:" + proxyJettyPort;
  }

  private class ProxyConfigHandler extends AbstractHandler {
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, ServletException {
      response.setContentType("text/xml;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      baseRequest.setHandled(true);
      try {
        response.getWriter().println(getTsaProxyConfig());
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
  }
}
