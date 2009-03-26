/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

public class ManualClientLockManagementTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }
  
  @Override
  protected Class getApplicationClass() {
    return ManualClientLockManagementTestApp.class;
  }
}
