/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.GroupData;
import com.tc.test.MultipleServerManager;
import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.util.PortChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActiveActiveServerManager extends MultipleServerManager {
  /**
   * One <code>ActivePassiveServerManager</code> for each group since they logically form a group
   */
  private ActivePassiveServerManager[] activePassiveServerManagers;
  private ProxyConnectManager[]        proxyL2Managers;
  private ProxyConnectManager[]        proxyL1Managers;
  private GroupData[]                  groupsData;

  public ActiveActiveServerManager(File tempDir, PortChooser portChooser, String configModel,
                                   ActiveActiveTestSetupManager setupManger, File javaHome,
                                   TestTVSConfigurationSetupManagerFactory configFactory, List extraJvmArgs,
                                   boolean isProxyL2GroupPorts, boolean isProxyDsoPorts) throws Exception {
    super(setupManger);
    int groupCount = setupManger.getActiveServerGroupCount();
    activePassiveServerManagers = new ActivePassiveServerManager[groupCount];
    String configFileLocation = tempDir + File.separator + CONFIG_FILE_NAME;
    File configFile = new File(configFileLocation);

    if (this.setupManger.getServerCount() < 2) { throw new AssertionError(
                                                                          "Active-test tests involve 2 or more DSO servers: serverCount=["
                                                                              + this.setupManger.getServerCount() + "]"); }

    int noOfServers = 0;
    for (int i = 0; i < groupCount; i++) {
      ActivePassiveTestSetupManager activePasssiveTestSetupManager = createActivePassiveTestSetupManager(i);
      activePassiveServerManagers[i] = new ActivePassiveServerManager(true, tempDir, portChooser, configModel,
                                                                      activePasssiveTestSetupManager, javaHome,
                                                                      configFactory, extraJvmArgs, isProxyL2GroupPorts,
                                                                      isProxyDsoPorts, true, noOfServers);
      noOfServers += setupManger.getGroupMemberCount(i);
    }

    if (isProxyL2GroupPorts) {
      setL2ProxyManagers();
    }

    groupsData = createGroups();
    // Create a active-active config creator and then write the config
    serverConfigCreator = new MultipleServersConfigCreator(this.setupManger, groupsData, configModel, configFile,
                                                           tempDir, configFactory);
    serverConfigCreator.writeL2Config();

    for (int i = 0; i < activePassiveServerManagers.length; i++) {
      activePassiveServerManagers[i].setConfigCreator(serverConfigCreator);
    }

    if (isProxyDsoPorts) {
      setL1ProxyManagers();

      for (int i = 0; i < activePassiveServerManagers.length; i++) {
        groupsData[i].setDsoPorts(activePassiveServerManagers[i].getProxyDsoPorts());
      }
    }
  }

  private GroupData[] createGroups() {
    // Create the groups by getting the server names from the ActivePassiveServerManager
    GroupData[] groups = new GroupData[activePassiveServerManagers.length];
    for (int i = 0; i < groups.length; i++) {
      groups[i] = new GroupData(activePassiveServerManagers[i].getDsoPorts(), activePassiveServerManagers[i]
          .getJmxPorts(), activePassiveServerManagers[i].getL2GroupPorts(), activePassiveServerManagers[i]
          .getServerNames());
    }
    return groups;
  }

  private void setL2ProxyManagers() {
    proxyL2Managers = new ProxyConnectManager[setupManger.getServerCount()];
    int count = 0;
    for (int i = 0; i < activePassiveServerManagers.length; i++) {
      ProxyConnectManager[] managers = activePassiveServerManagers[i].getL2ProxyManagers();
      for (int j = 0; j < managers.length; j++) {
        proxyL2Managers[count] = managers[j];
        count++;
      }
    }
  }

  private void setL1ProxyManagers() {
    proxyL1Managers = new ProxyConnectManager[setupManger.getServerCount()];
    int count = 0;
    for (int i = 0; i < activePassiveServerManagers.length; i++) {
      ProxyConnectManager[] managers = activePassiveServerManagers[i].getL1ProxyManagers();
      for (int j = 0; j < managers.length; j++) {
        proxyL1Managers[count] = managers[j];
        count++;
      }
    }
  }

  private ActivePassiveTestSetupManager createActivePassiveTestSetupManager(int grpIndex) {
    ActivePassiveTestSetupManager testSetupManager = new ActivePassiveTestSetupManager();
    testSetupManager.setMaxCrashCount(setupManger.getMaxCrashCount());
    testSetupManager.setServerCount(setupManger.getGroupMemberCount(grpIndex));
    ActiveActiveCrashMode mode = new ActiveActiveCrashMode(setupManger.getServerCrashMode());
    testSetupManager.setServerCrashMode(mode);
    testSetupManager.setServerCrashWaitTimeInSec(setupManger.getServerCrashWaitTimeInSec());
    testSetupManager.setServerPersistenceMode(setupManger.getServerPersistenceMode());
    testSetupManager.setServerShareDataMode(setupManger.getGroupServerShareDataMode(grpIndex));

    return testSetupManager;
  }

  public ProxyConnectManager[] getL2ProxyManagers() {
    return proxyL2Managers;
  }

  public void startActiveActiveServers() throws Exception {
    int grpCount = setupManger.getActiveServerGroupCount();
    Thread[] threads = new Thread[grpCount];
    for (int i = 0; i < grpCount; i++) {
      final ActivePassiveServerManager serverManager = activePassiveServerManagers[i];

      Runnable r = new Runnable() {
        public void run() {
          try {
            serverManager.startActivePassiveServers();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };

      threads[i] = new Thread(r);
    }

    for (int i = 0; i < grpCount; i++)
      threads[i].start();
  }

  public List getErrors() {
    List l = new ArrayList();
    int grpCount = setupManger.getActiveServerGroupCount();

    for (int i = 0; i < grpCount; i++) {
      l.addAll(activePassiveServerManagers[i].getErrors());
    }
    return l;
  }

  public void stopAllServers() throws Exception {
    int grpCount = setupManger.getActiveServerGroupCount();

    for (int i = 0; i < grpCount; i++) {
      activePassiveServerManagers[i].stopAllServers();
    }
  }

  public void dumpAllServers(int currentPid, int dumpCount, long dumpInterval) throws Exception {
    int grpCount = setupManger.getActiveServerGroupCount();

    for (int i = 0; i < grpCount; i++) {
      activePassiveServerManagers[i].dumpAllServers(currentPid, dumpCount, dumpInterval);
    }
  }

  public void addGroupsToL1Config(TestTVSConfigurationSetupManagerFactory configFactory) {
    configFactory.addServersAndGroupsToL1Config(groupsData);
  }

  public void crashActiveServers() throws Exception {
    int grpCount = setupManger.getActiveServerGroupCount();

    for (int i = 0; i < grpCount; i++) {
      activePassiveServerManagers[i].crashActive();
    }
  }

  public ProxyConnectManager[] getL1ProxyManagers() {
    return proxyL1Managers;
  }
}
