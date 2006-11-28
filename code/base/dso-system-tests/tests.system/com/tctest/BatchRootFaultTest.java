/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class BatchRootFaultTest extends TransparentTestBase {

  public static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
    // t.getL2DSOConfig().setCacheSize(400000);
  }

  protected Class getApplicationClass() {
    return BatchRootFaultTestApp.class;
  }

  protected boolean getStartServer() {
    return true;
  }
}
