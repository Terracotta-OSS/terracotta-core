/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class TransparentTransientTest extends TransparentTestBase {
  private static final int NODE_COUNT           = 2;
  private static final int LOOP_ITERATION_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TransparentTransientTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}