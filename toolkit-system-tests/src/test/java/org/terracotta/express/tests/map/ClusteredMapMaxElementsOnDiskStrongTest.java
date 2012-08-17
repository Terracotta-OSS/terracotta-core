/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.ToolkitTestConfigHelper;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;

import java.util.List;

public class ClusteredMapMaxElementsOnDiskStrongTest extends AbstractToolkitTestBase {

  public ClusteredMapMaxElementsOnDiskStrongTest(TestConfig testConfig) {
    super(testConfig, Client.class);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(60);
    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED, "true");

    testConfig.addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");

    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT, "0.001");

    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED, "true");
    testConfig.getClientConfig().setMaxHeap(512);

    testConfig.getClientConfig().addExtraClientJvmArg("-Dnet.sf.ehcache.sizeof.verboseDebugLogging=true");
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    return ToolkitTestConfigHelper.getOSConfigs();
  }

  public static class Client extends ClusteredMapMaxElementsOnDiskTestClient {

    public Client(String[] args) {
      super(args);
    }

    @Override
    public Consistency getConsistency() {
      return Consistency.STRONG;
    }

  }

}
