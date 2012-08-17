/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.ToolkitTestConfigHelper;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;

import java.util.List;

public class ClusteredMapPinningStrongOSTest extends AbstractToolkitTestBase {
  private static final int NODE_COUNT = 8;

  public ClusteredMapPinningStrongOSTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(PinningTestClient.class, NODE_COUNT);
    testConfig.getClientConfig().addExtraClientJvmArg("-D" + PinningTestClient.CONSISTENCY + "=" + Consistency.STRONG);
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    return ToolkitTestConfigHelper.getOSConfigs();
  }

}
