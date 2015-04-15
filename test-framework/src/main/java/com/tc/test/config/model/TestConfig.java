/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test.config.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for creating all configurations for a test. It has individual members who take care of
 * specifying specific sets of config for a test
 * 
 * @author rsingh
 */
public class TestConfig {

  private int                                        numOfGroups             = 1;
  private final String                               configName;
  private final L2Config                             l2Config                = new L2Config();
  private final Map<Integer, Map<Integer, L2Config>> serverSpecificL2Configs = new HashMap<Integer, Map<Integer, L2Config>>();
  private final CrashConfig                          crashConfig             = new CrashConfig();
  private final GroupConfig                          groupConfig             = new GroupConfig();
  private final ClientConfig                         clientConfig            = new ClientConfig();
  private final Map<String, String>                  tcPropertiesMap;
  private boolean                                    isStandAloneTest        = false;
  private Boolean                                    restartZappedL2;
  private Boolean                                    dgcEnabled              = null; // 'null' means gc configuration is omitted
  private int                                        dgcIntervalInSec        = 3600;
  private boolean                                    dgcVerbose              = false;
  private boolean                                    restartable             = false;
  private int                                        clientReconnectWindow   = 120;
  private boolean                                    pauseFeatureEnabled     = false;

  public TestConfig(String configName) {
    this.configName = configName;
    this.tcPropertiesMap = new HashMap<String, String>();
  }

  /**
   * Returns the configuration for each L2 in the test. <br>
   * Note that any change done via this object will reflect in all the L2s of the test
   * 
   * @return the L2 config for the test.
   */
  public L2Config getL2Config() {
    return l2Config;
  }

  /**
   * Get the L2Config for a specific L2. If no specific config exists, just return the default one.
   *
   * @param groupIndex group of the L2
   * @param serverIndex index in the group for the L2
   * @return the L2's config
   */
  public L2Config getL2Config(int groupIndex, int serverIndex) {
    Map<Integer, L2Config> groupConfigs = serverSpecificL2Configs.get(groupIndex);
    if (groupConfigs == null) {
      return getL2Config();
    }
    L2Config serverConfig = groupConfigs.get(serverIndex);
    if (serverConfig == null) {
      return getL2Config();
    }
    return serverConfig;
  }

  /**
   * Get the L2Config for a specific L2, creating a config if no specific config exists.
   *
   * @param groupIndex group of the L2
   * @param serverIndex index in the group of the L2
   * @return L2's config
   */
  public L2Config getOrCreateSpecificL2Config(int groupIndex, int serverIndex) {
    Map<Integer, L2Config> groupConfigs = serverSpecificL2Configs.get(groupIndex);
    if (groupConfigs == null) {
      groupConfigs = new HashMap<Integer, L2Config>();
      serverSpecificL2Configs.put(groupIndex, groupConfigs);
    }
    L2Config serverConfig = groupConfigs.get(serverIndex);
    if (serverConfig == null) {
      serverConfig = new L2Config();
      groupConfigs.put(serverIndex, serverConfig);
    }
    return serverConfig;
  }

  /**
   * Returns the crash configuration for the test
   */
  public CrashConfig getCrashConfig() {
    return crashConfig;
  }

  /**
   * Returns the group configuration for each group <br>
   * note that any change done via this object will reflect in each mirror group of the test configuration
   * 
   * @return
   */
  public GroupConfig getGroupConfig() {
    return groupConfig;
  }

  /**
   * Returns the configuration for each client. <br>
   * Note that any change done via this object will reflect in each client of the test
   * 
   * @return
   */
  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  /**
   * @return number of mirror groups in the test
   */
  public int getNumOfGroups() {
    return numOfGroups;
  }

  /**
   * The map containing tc-properties to be overwritten <br>
   * note that these properties will added to the tc-config for each server and client
   * 
   * @return
   */
  public Map<String, String> getTcPropertiesMap() {
    return tcPropertiesMap;
  }

  /**
   * Adds a tc property in the config for each server and client
   * 
   * @param key : tc property key
   * @param value : tc property value
   */
  public void addTcProperty(String key, String value) {
    tcPropertiesMap.put(key, value);
  }

  /**
   * Sets the number of mirror groups for test. Default is one, each having one server
   * 
   * @param numOfGroups : number ofr mirror groups to be present for the test
   */
  public void setNumOfGroups(int numOfGroups) {
    this.numOfGroups = numOfGroups;
  }

  /**
   * @return : true if the test is supposed to be run in standAlone mode
   */
  public boolean isStandAloneTest() {
    return isStandAloneTest;
  }

  public boolean isRestartZappedL2() {
    // By default, restart zapped servers in stripes with multiple passives
    if (restartZappedL2 == null) { return groupConfig.getMemberCount() > 2; }
    return restartZappedL2;
  }

  /**
   * enables or disable the test in standAlone mode. Note that if this is set the servers will not start and so the user
   * has to make sure that there are no errors like crashing the servers, or using clustered barrier etc.
   * 
   * @param
   */
  public void setStandAloneTest(boolean isStandAloneTest) {
    this.isStandAloneTest = isStandAloneTest;
  }

  public void setRestartZappedL2(boolean restart) {
    restartZappedL2 = restart;
  }

  /**
   * creates a default test configuration object for the test
   * 
   * @param configName : name of the config
   * @param numOfGroups : how many mirror gorups should be present in the test
   * @param numOfServersPerGroup : how many server in each mirror group should be present for the test
   * @return
   */
  public static TestConfig createTestConfig(final String configName, final int numOfGroups,
                                            final int numOfServersPerGroup) {
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(numOfGroups);
    testConfig.getGroupConfig().setMemberCount(numOfServersPerGroup);
    return testConfig;
  }

  /**
   * @return the name of the test config
   */
  public String getConfigName() {
    return configName;
  }

  /**
   * Creates a test config with one mirror group having one server as member
   * 
   * @return
   */
  public static TestConfig createSingleServerConfig() {
    return createTestConfig("SingleServerConfig", 1, 1);
  }

  /**
   * Creates a test config with one mirror group having two servers in as member
   * 
   * @return
   */
  public static TestConfig createOneActiveOnePassiveConfig() {
    return createTestConfig("OneActiveOnePassive", 1, 2);
  }

  /**
   * Creates a test config with two mirror groups having two servers each as member
   * 
   * @return
   */
  public static TestConfig createTwoActiveTwoPassiveConfig() {
    return createTestConfig("TwoActiveTwoPassive", 2, 2);
  }

  /**
   * Persistence mode for the L2
   */
  public boolean getRestartable() {
    return restartable;
  }

  /**
   * Sets whether the L2 should be restartable
   * 
   * @param restartable true to enable restartable
   */
  public void setRestartable(boolean restartable) {
    this.restartable = restartable;
  }

  /**
   * client reconnect window in secs
   */
  public int getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  /**
   * sets client reconnect window in seconds
   */
  public void setClientReconnectWindow(int clientReconnectWindow) {
    this.clientReconnectWindow = clientReconnectWindow;
  }

  public void setDgcEnabled(Boolean b) {
    this.dgcEnabled = b;
  }

  public Boolean isDgcEnabled() {
    return this.dgcEnabled;
  }

  public void setDgcIntervalInSec(int i) {
    this.dgcIntervalInSec = i;
  }

  public int getDgcIntervalInSec() {
    return this.dgcIntervalInSec;
  }

  public boolean getDgcVerbose() {
    return this.dgcVerbose;
  }

  public void setDgcVerbose(boolean dgcVerbose) {
    this.dgcVerbose = dgcVerbose;
  }

  public boolean isPauseFeatureEnabled() {
    return pauseFeatureEnabled;
  }

  public void setPauseFeatureEnabled(boolean pauseFeatureEnabled) {
    this.pauseFeatureEnabled = pauseFeatureEnabled;
  }
}
