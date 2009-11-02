/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ManualClientLockManagementTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  protected void setUp() throws Exception {
    super.setUp();
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL, "1000");
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }
  
  @Override
  protected Class getApplicationClass() {
    return ManualClientLockManagementTestApp.class;
  }
}
