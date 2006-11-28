/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class OnLoadTest extends TransparentTestBase {

  public static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return OnLoadTestApp.class;
  }

  protected boolean getStartServer() {
    return true;
  }

}
