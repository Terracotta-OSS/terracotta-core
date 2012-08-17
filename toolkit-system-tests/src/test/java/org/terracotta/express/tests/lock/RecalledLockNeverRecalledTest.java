/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.apache.commons.io.IOUtils;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

public class RecalledLockNeverRecalledTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  protected String getTestDependencies() {
    return TestBaseUtil.jarFor(IOUtils.class);
  }

  public RecalledLockNeverRecalledTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(RecalledLockNeverRecalledTestClient.class, NODE_COUNT);
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc." + TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL
                                                          + "=5000");
    testConfig.addTcProperty(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL, "5000");
  }

}
