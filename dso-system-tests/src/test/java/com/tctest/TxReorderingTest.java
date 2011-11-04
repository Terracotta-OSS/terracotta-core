/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;


public class TxReorderingTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public TxReorderingTest() {
    timebombTest("2013-01-15");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return TxReorderingApp.class;
  }
}
