/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class ToolkitTestConfigHelper {

  public static TestConfig createActivePassiveConfig(PersistenceMode persistenceMode) {
    String configName = "active-passive-"
                        + (persistenceMode == PersistenceMode.PERMANENT_STORE ? "perm-store" : "temp-swap");
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setPersistenceMode(persistenceMode);
    return testConfig;
  }

  public static TestConfig createActiveActiveConfig(PersistenceMode persistenceMode) {
    String configName = "active-active-"
                        + (persistenceMode == PersistenceMode.PERMANENT_STORE ? "perm-store" : "temp-swap");
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(2);
    testConfig.getGroupConfig().setMemberCount(1);
    testConfig.getL2Config().setPersistenceMode(persistenceMode);
    return testConfig;
  }

  public static TestConfig createActivePassiveCrashConfig(PersistenceMode persistenceMode, ServerCrashMode crashMode,
                                                          int serverCrashIntervalSecs) {
    String configName = "active-passive-crash-"
                        + (persistenceMode == PersistenceMode.PERMANENT_STORE ? "perm-store" : "temp-swap");
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setPersistenceMode(persistenceMode);
    testConfig.getCrashConfig().setCrashMode(crashMode);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(serverCrashIntervalSecs);
    return testConfig;
  }

  public static TestConfig createActiveActiveCrashConfig(PersistenceMode persistenceMode, ServerCrashMode crashMode,
                                                         int serverCrashIntervalSecs) {
    String configName = "active-active-crash-"
                        + (persistenceMode == PersistenceMode.PERMANENT_STORE ? "perm-store" : "temp-swap");
    TestConfig testConfig = new TestConfig(configName);
    testConfig.setNumOfGroups(2);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setPersistenceMode(persistenceMode);
    testConfig.getCrashConfig().setCrashMode(crashMode);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(serverCrashIntervalSecs);
    return testConfig;
  }

  public static List<TestConfig> getOSConfigs() {
    List<TestConfig> testConfigs = new ArrayList<TestConfig>();

    testConfigs.add(createActivePassiveConfig(PersistenceMode.PERMANENT_STORE));
    testConfigs.add(createActivePassiveConfig(PersistenceMode.TEMPORARY_SWAP_ONLY));

    testConfigs.add(createActivePassiveCrashConfig(PersistenceMode.PERMANENT_STORE,
                                                   ServerCrashMode.RANDOM_SERVER_CRASH, 30));

    testConfigs.add(createActivePassiveCrashConfig(PersistenceMode.TEMPORARY_SWAP_ONLY,
                                                   ServerCrashMode.RANDOM_SERVER_CRASH, 30));
    return testConfigs;
  }

  public static List<TestConfig> getEEConfigs() {
    List<TestConfig> testConfigs = new ArrayList<TestConfig>();

    testConfigs.add(createActiveActiveConfig(PersistenceMode.PERMANENT_STORE));
    testConfigs.add(createActiveActiveConfig(PersistenceMode.TEMPORARY_SWAP_ONLY));

    testConfigs.add(createActiveActiveCrashConfig(PersistenceMode.PERMANENT_STORE, ServerCrashMode.RANDOM_SERVER_CRASH,
                                                  30));
    testConfigs.add(createActiveActiveCrashConfig(PersistenceMode.TEMPORARY_SWAP_ONLY,
                                                  ServerCrashMode.RANDOM_SERVER_CRASH, 30));

    return testConfigs;
  }

  public static List<TestConfig> getAllConfigs() {
    List<TestConfig> testConfigs = new ArrayList<TestConfig>();
    testConfigs.addAll(getOSConfigs());
    testConfigs.addAll(getEEConfigs());
    return testConfigs;
  }
}
