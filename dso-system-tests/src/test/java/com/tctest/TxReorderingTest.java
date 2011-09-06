/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import java.util.Date;

public class TxReorderingTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public TxReorderingTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TxReorderingApp.class;
  }
}
