/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class SubtypeMatchingTest extends TransparentTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SubtypeMatchingTestApp.class;
  }

  protected boolean canRunCrash() {
    return false;
  }

}
