/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.test.runner.TcTestRunner.Configs;

import java.util.ArrayList;
import java.util.List;

public class L2DumperTest extends AbstractToolkitTestBase {

  public L2DumperTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Configs
  public static List<TestConfig> getTestConfigs() {
    List<TestConfig> configList = new ArrayList<TestConfig>();
    TestConfig testConfig = new TestConfig("SimpleConfig");
    testConfig.getGroupConfig().setMemberCount(1);
    testConfig.getClientConfig().setClientClasses(L2DumperTestClient.class, 1);
    configList.add(testConfig);

    TestConfig offHeapConfig = new TestConfig("OffHeapConfig");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK, "true");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE, "1m");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK, "true");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE, "1m");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_TABLESIZE, "1m");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY, "16");
    offHeapConfig.getClientConfig().setDirectMemorySize(256);

    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MAX_PAGE_SIZE, "10k");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MIN_PAGE_SIZE, "10k");
    offHeapConfig.addTcProperty(TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_TABLESIZE, "1k");
    configList.add(offHeapConfig);
    return configList;
  }

}
