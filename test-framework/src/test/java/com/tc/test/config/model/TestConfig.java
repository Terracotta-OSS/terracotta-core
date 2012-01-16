package com.tc.test.config.model;

import java.util.HashMap;
import java.util.Map;

public class TestConfig {

  private int                       numOfGroups  = 1;
  private final String              configName;
  private final L2Config            l2Config     = new L2Config();
  private final CrashConfig         crashConfig  = new CrashConfig();
  private final GroupConfig         groupConfig  = new GroupConfig();
  private final ClientConfig        clientConfig = new ClientConfig();
  private final Map<String, String> tcPropertiesMap;

  public TestConfig(String configName) {
    this.configName = configName;
    this.tcPropertiesMap = new HashMap<String, String>();
  }

  public L2Config getL2Config() {
    return l2Config;
  }

  public CrashConfig getCrashConfig() {
    return crashConfig;
  }

  public GroupConfig getGroupConfig() {
    return groupConfig;
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  public int getNumOfGroups() {
    return numOfGroups;
  }

  public Map<String, String> getTcPropertiesMap() {
    return tcPropertiesMap;
  }

  public void addTcProperty(String key, String value) {
    tcPropertiesMap.put(key, value);
  }

  public void setNumOfGroups(int numOfGroups) {
    this.numOfGroups = numOfGroups;
  }

  public static TestConfig createTestConfig(final String configName, final int numOfGroups,
                                            final int numOfServersPerGroup) {
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(numOfGroups);
    testConfig.getGroupConfig().setMemberCount(numOfServersPerGroup);
    return testConfig;
  }

  public String getConfigName() {
    return configName;
  }

  public static TestConfig createSingleServerConfig() {
    return createTestConfig("SingleServerConfig", 1, 1);
  }

  public static TestConfig createOneActiveOnePassiveConfig() {
    return createTestConfig("OneActiveOnePassive", 1, 2);
  }

  public static TestConfig createTwoActiveTwoPassiveConfig() {
    return createTestConfig("TwoActiveTwoPassive", 2, 2);
  }

}
