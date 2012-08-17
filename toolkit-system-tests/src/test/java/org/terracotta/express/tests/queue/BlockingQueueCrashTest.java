/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.List;

// import com.tc.util.runtime.Vm;

public class BlockingQueueCrashTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 8;

  public BlockingQueueCrashTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(BlockingQueueCrashTestClient.class, NODE_COUNT);
  }

  @Configs
  public static List<TestConfig> getConfigs() {
    List<TestConfig> testConfigs = new ArrayList<TestConfig>();
    TestConfig noCrashConfig = new TestConfig("NoCrashConfig");
    testConfigs.add(noCrashConfig);
    // Active Passive Config
    TestConfig activePassiveConfig = new TestConfig("APConfig");
    activePassiveConfig.getGroupConfig().setMemberCount(2);
    activePassiveConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    activePassiveConfig.getCrashConfig().setServerCrashWaitTimeInSec(30);
    activePassiveConfig.getL2Config().setClientReconnectWindow(20);
    activePassiveConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "false");
    testConfigs.add(activePassiveConfig);

    TestConfig activePassiveL1ReconnectConfig = new TestConfig("APL1ReconnectConfig");
    activePassiveL1ReconnectConfig.getGroupConfig().setMemberCount(2);
    activePassiveL1ReconnectConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    activePassiveL1ReconnectConfig.getCrashConfig().setServerCrashWaitTimeInSec(30);
    activePassiveL1ReconnectConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    if (Os.isLinux() || Os.isSolaris()) {
      // default 5000 ms seems to small occasionally in few linux machines
      activePassiveL1ReconnectConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "10000");
    }
    testConfigs.add(activePassiveL1ReconnectConfig);

    return testConfigs;
  }

}
