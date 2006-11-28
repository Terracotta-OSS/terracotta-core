/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class TreeMapTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TreeMapTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
