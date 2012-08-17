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

public class BlockingQueueTest extends AbstractExpressActivePassiveTest {
  public BlockingQueueTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Configs
  public static List<TestConfig> getConfigs() {
    List<TestConfig> configs = new ArrayList<TestConfig>();
    TestConfig basicConfig = new TestConfig("basic");
    basicConfig.getClientConfig().setClientClasses(BlockingQueueTestClient.class, 10);
    configs.add(basicConfig);
    TestConfig gcConfig = new TestConfig("gcConfig");
    gcConfig.getClientConfig().setClientClasses(BlockingQueueTestClient.class, 10);
    gcConfig.getL2Config().setDgcEnabled(true);
    configs.add(gcConfig);
    TestConfig youngGenConfig = new TestConfig("youngGC");
    youngGenConfig.getClientConfig().setClientClasses(BlockingQueueTestClient.class, 10);
    youngGenConfig.getL2Config().setDgcEnabled(true);
    youngGenConfig.addTcProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED, "true");
    youngGenConfig.addTcProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY, "10000");

    return configs;

  }
}
