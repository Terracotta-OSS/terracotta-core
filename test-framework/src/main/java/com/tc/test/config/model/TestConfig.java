package com.tc.test.config.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for creating all configurations for a test. It has
 * individual members who take care of specifying specific sets of config for a
 * test
 * 
 * @author rsingh
 */
public class TestConfig {

  private int numOfGroups = 1;
  private final String configName;
  private final L2Config l2Config = new L2Config();
  private final CrashConfig crashConfig = new CrashConfig();
  private final GroupConfig groupConfig = new GroupConfig();
  private final ClientConfig clientConfig = new ClientConfig();
  private final Map<String, String> tcPropertiesMap;

  public TestConfig(String configName) {
    this.configName = configName;
    this.tcPropertiesMap = new HashMap<String, String>();
  }

  /**
   * Returns the configuration for each L2 in the test. <br>
   * Note that any change done via this object will reflect in all the L2s of
   * the test
   * 
   * @return the L2 config for the test.
   */
  public L2Config getL2Config() {
    return l2Config;
  }

  /**
   * Returns the crash configuration for the test
   */
  public CrashConfig getCrashConfig() {
    return crashConfig;
  }

  /**
   * Returns the group configuration for each group <br>
   * note that any change done via this object will reflect in each mirror
   * group of the test configuration
   * 
   * @return
   */
  public GroupConfig getGroupConfig() {
    return groupConfig;
  }

  /**
   * Returns the configuration for each client. <br>
   * Note that any change done via this object will reflect in each client of
   * the test
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
   * note that these properties will added to the tc-config for each server
   * and client
   * 
   * @return
   */
  public Map<String, String> getTcPropertiesMap() {
    return tcPropertiesMap;
  }

  /**
   * Adds a tc property in the config for each server and client
   * 
   * @param key
   *            : tc property key
   * @param value
   *            : tc property value
   */
  public void addTcProperty(String key, String value) {
    tcPropertiesMap.put(key, value);
  }

  /**
   * Sets the number of mirror groups for test. Default is one, each having
   * one server
   * 
   * @param numOfGroups
   *            : number ofr mirror groups to be present for the test
   */
  public void setNumOfGroups(int numOfGroups) {
    this.numOfGroups = numOfGroups;
  }

  /**
   * creates a default test configuration object for the test
   * 
   * @param configName
   *            : name of the config
   * @param numOfGroups
   *            : how many mirror gorups should be present in the test
   * @param numOfServersPerGroup
   *            : how many server in each mirror group should be present for
   *            the test
   * @return
   */
  public static TestConfig createTestConfig(final String configName,
      final int numOfGroups, final int numOfServersPerGroup) {
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
   * Creates a test config with one mirror group having two servers in as
   * member
   * 
   * @return
   */
  public static TestConfig createOneActiveOnePassiveConfig() {
    return createTestConfig("OneActiveOnePassive", 1, 2);
  }

  /**
   * Creates a test config with two mirror groups having two servers each as
   * member
   * 
   * @return
   */
  public static TestConfig createTwoActiveTwoPassiveConfig() {
    return createTestConfig("TwoActiveTwoPassive", 2, 2);
  }

}
