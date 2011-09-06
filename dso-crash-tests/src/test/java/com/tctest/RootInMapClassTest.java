/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class RootInMapClassTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 5;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return RootInRootClassTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
