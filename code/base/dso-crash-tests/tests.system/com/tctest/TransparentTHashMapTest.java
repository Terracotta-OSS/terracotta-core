/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class TransparentTHashMapTest extends TransparentTestBase {

  private static final int NODE_COUNT = 6;

  protected Class getApplicationClass() {
    return TransparentTHashMapTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }

}
