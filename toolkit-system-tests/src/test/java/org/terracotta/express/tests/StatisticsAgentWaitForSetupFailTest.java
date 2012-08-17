/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.config.model.TestConfig;

public class StatisticsAgentWaitForSetupFailTest extends AbstractToolkitTestBase {

  public StatisticsAgentWaitForSetupFailTest(TestConfig testConfig) {
    super(testConfig, StatisticsAgentWaitForSetupFailTestApp.class, StatisticsAgentWaitForSetupFailTestApp.class);
    final String fail_buffer_open_sysprop = TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN);
    testConfig.addTcProperty(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN, "true");
    System.setProperty(fail_buffer_open_sysprop, "true");
    testConfig.getL2Config().addExtraServerJvmArg("-D" + fail_buffer_open_sysprop + "=true");
  }

}
