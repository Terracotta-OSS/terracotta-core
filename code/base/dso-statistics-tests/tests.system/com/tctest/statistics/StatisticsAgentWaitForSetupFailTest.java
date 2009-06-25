/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import java.util.ArrayList;

public class StatisticsAgentWaitForSetupFailTest extends TransparentTestBase {

  @Override
  protected Class getApplicationClass() {
    return StatisticsAgentWaitForSetupFailTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsAgentWaitForSetupFailTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setJvmArgsCvtIsolation(final ArrayList jvmArgs) {
    final String fail_buffer_open_sysprop = TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN);
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN, "true");
    System.setProperty(fail_buffer_open_sysprop, "true");

    jvmArgs.add("-D" + fail_buffer_open_sysprop + "=true");
  }
}