/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class LockUpgradeSystemTest extends TransparentTestBase implements TestConfigurator {

  // WARNING: Setting this to anything greater than 1 will cause lock upgrade deadlock
  private static final int NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LockUpgradeSystemTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
