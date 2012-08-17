/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;

import java.util.ArrayList;
import java.util.List;

public class BlockingQueueInterruptTakeTest extends AbstractExpressActivePassiveTest {

  public BlockingQueueInterruptTakeTest(TestConfig testConfig) {
    super(testConfig, BlockingQueueInterruptTakeTestClient.class, BlockingQueueInterruptTakeTestClient.class,
          BlockingQueueInterruptTakeTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);

    disableTest();
  }

  @Configs
  public static List<TestConfig> getConfigs() {
    List<TestConfig> configs = new ArrayList<TestConfig>();
    TestConfig basicConfig = new TestConfig("basicConfig");
    basicConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    configs.add(basicConfig);

    TestConfig reconectDisabledConfig = new TestConfig("reconnectDisabledConfig");
    reconectDisabledConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "false");
    configs.add(reconectDisabledConfig);

    return configs;

  }
}
