/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class PrimitiveArrayTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 16; // MUST BE 16 (8 eight primitive

  // types, and 8 wrappers)

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return PrimitiveArrayTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
