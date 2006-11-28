/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
