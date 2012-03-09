package com.tc.test.setup;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.stats.api.DGCMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.config.model.TestConfig;
import com.tc.util.PortChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

public class TestServerManager {
  private final TestConfig           testConfig;
  private final PortChooser          portChooser;
  private final GroupServerManager[] groups;
  private final ConfigHelper         configHelper;
  private static final boolean       DEBUG = Boolean.getBoolean("test.framework.debug");

  public TestServerManager(TestConfig testConfig, File tempDir, File tcConfigFile, File javaHome) throws Exception {
    this.testConfig = testConfig;
    portChooser = new PortChooser();

    this.configHelper = new ConfigHelper(portChooser, testConfig, tcConfigFile, tempDir);
    this.configHelper.writeConfigFile();

    final int numOfGroups = testConfig.getNumOfGroups();
    this.groups = new GroupServerManager[numOfGroups];

    for (int groupIndex = 0; groupIndex < this.groups.length; groupIndex++) {
      this.groups[groupIndex] = new GroupServerManager(configHelper.getGroupData(groupIndex), testConfig, tempDir,
                                                       javaHome, tcConfigFile);
    }
  }

  public void startAllServers() throws Exception {
    int grpCount = testConfig.getNumOfGroups();
    Thread[] threads = new Thread[grpCount];
    for (int i = 0; i < grpCount; i++) {
      final GroupServerManager serverManager = groups[i];

      Runnable r = new Runnable() {
        public void run() {
          try {
            serverManager.startAllServers();
            serverManager.startCrasher();
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
      threads[i].join();
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
    debugPrintln("***** stoppig server crashers");
    int grpCount = testConfig.getNumOfGroups();
    for (int i = 0; i < grpCount; i++) {
      groups[i].stopCrasher();
    }

    for (int i = 0; i < grpCount; i++) {
      groups[i].stopAllServers();
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
    debugPrintln("***** dumping ClusterState ");
    int grpCount = testConfig.getNumOfGroups();
    boolean dumpTaken = false;
    List<Exception> errors = new ArrayList<Exception>();
    for (int i = 0; i < grpCount; i++) {
      try {
        dumpTaken = groups[i].dumpClusterState(dumpCount, dumpInterval);
      } catch (Exception e) {
        System.out.println("Error while taking cluster dump");
        errors.add(e);
        e.printStackTrace();
      }
      if (dumpTaken) {
        break;
      }
    }
    if (!dumpTaken) {
      System.out.println("******* Dumping ClusterState Failed. Printing All Exceptions");
      for (Exception exception : errors) {
        exception.printStackTrace();
      }
    }

  }

  public GroupsData getGroupData(int groupIndex) {
    return groups[groupIndex].getGroupData();
  }

  public void crashAllPassive(int groupIndex) throws Exception {
    groups[groupIndex].crashAllPassive();
  }

  public void crashServer(int groupIndex, int serverIndex) throws Exception {
    groups[groupIndex].crashServer(serverIndex);
  }

  public boolean isActivePresent(int groupIndex) {
    return groups[groupIndex].isActivePresent();

  }

  public boolean isPassiveStandBy(int groupIndex) {
    return groups[groupIndex].isPassiveStandBy();

  }

  public void waitUntilPassiveStandBy(int groupIndex) throws Exception {
    groups[groupIndex].waituntilPassiveStandBy();
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
  public void closeClientConnections(int groupIndex) {
    Assert.assertTrue(groupIndex >= 0 && groupIndex < groups.length);
    this.groups[groupIndex].stopDsoProxy();
  }
}