/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import java.util.Date;

public class DEV3307Test extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
  
  public DEV3307Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return DEV3307TestApp.class;
  }

}
