/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;
import com.tc.util.runtime.Os;

import java.util.Arrays;
import java.util.List;

public class ServerCrashAndRestartTest extends AbstractToolkitTestBase {

  public ServerCrashAndRestartTest(TestConfig testConfig) {
    super(testConfig, ServerCrashAndRestartTestApp.class, ServerCrashAndRestartTestApp.class,
          ServerCrashAndRestartTestApp.class, ServerCrashAndRestartTestApp.class, ServerCrashAndRestartTestApp.class);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    TestConfig noL1ReconnectConfig = new TestConfig("no-l1-reconnect");
    noL1ReconnectConfig.getGroupConfig().setMemberCount(1);

    TestConfig l1ReconnectConfig = new TestConfig("l1-reconnect");
    l1ReconnectConfig.getGroupConfig().setMemberCount(1);
    l1ReconnectConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");

    if (Os.isLinux() || Os.isSolaris()) {
      // default 5000 ms seems to small occasionally in few linux machines
      l1ReconnectConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "10000");
    }
    TestConfig[] testConfigs = new TestConfig[] { noL1ReconnectConfig, l1ReconnectConfig };
    return Arrays.asList(testConfigs);
  }

}
