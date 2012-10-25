/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

public class LockStatisticsJMXTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  protected String getTestDependencies() {
    return TestBaseUtil.jarFor(ConcurrentHashMap.class);
  }

  public LockStatisticsJMXTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(LockStatisticsJMXTestClient.class, NODE_COUNT);
    testConfig.addTcProperty(TCPropertiesConsts.LOCK_STATISTICS_ENABLED, "true");
    testConfig.addTcProperty("lock.statistics.max", "1000");
    testConfig.addTcProperty("l1.lock.stacktrace.defaultDepth", "1");
    testConfig.addTcProperty("l1.lock.collectFrequency", "10");

    disableTest();
  }

}
