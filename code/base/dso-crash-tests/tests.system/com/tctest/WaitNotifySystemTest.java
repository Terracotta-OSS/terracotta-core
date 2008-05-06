/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

/**
 * System test for distributed wait/notify
 */
public class WaitNotifySystemTest extends TransparentTestBase {
  private static final int NODE_COUNT = 4;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return WaitNotifySystemTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
