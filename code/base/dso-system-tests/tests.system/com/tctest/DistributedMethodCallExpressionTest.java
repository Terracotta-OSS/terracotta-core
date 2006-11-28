/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class DistributedMethodCallExpressionTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT           = 3;
  private static final int LOOP_ITERATION_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }
  
  protected Class getApplicationClass() {
    return DistributedMethodCallExpressionTestApp.class;
  }
}