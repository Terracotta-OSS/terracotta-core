/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

public class RecallUnderConcurrentLockTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 2;

  protected Class getApplicationClass() {
    return RecallUnderConcurrentLockTestApp.class;
  }
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }
}
