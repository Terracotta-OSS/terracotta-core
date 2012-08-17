/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;

import java.util.Arrays;
import java.util.List;

public class ClusteredMapDeleteValueOnRemoveTest extends AbstractToolkitTestBase {
  public ClusteredMapDeleteValueOnRemoveTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapDeleteValueOnRemoveTestClient.class, ClusteredMapDeleteValueOnRemoveTestClient.class);
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    TestConfig singleServerConfig = new TestConfig("SingleServerConfig");
    singleServerConfig.getGroupConfig().setMemberCount(1);
    singleServerConfig.addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL,
                                     "1000");

    TestConfig activePassiveCrashConfig = new TestConfig("active-passive-crash-conifg");
    activePassiveCrashConfig.getGroupConfig().setMemberCount(2);
    activePassiveCrashConfig
        .addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");
    activePassiveCrashConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    activePassiveCrashConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    activePassiveCrashConfig.getL2Config().setDgcEnabled(false);

    TestConfig activePassiveWithDgcConfig = new TestConfig("active-passive-periodic-dgc-config");
    activePassiveWithDgcConfig
        .addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");
    activePassiveWithDgcConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    activePassiveWithDgcConfig.getCrashConfig().setCrashMode(ServerCrashMode.NO_CRASH);
    activePassiveWithDgcConfig.getL2Config().setDgcEnabled(true);
    activePassiveWithDgcConfig.getL2Config().setDgcIntervalInSec(15);
    TestConfig[] testConfigs = new TestConfig[] { singleServerConfig, activePassiveCrashConfig,
        activePassiveWithDgcConfig };
    return Arrays.asList(testConfigs);
  }

}
