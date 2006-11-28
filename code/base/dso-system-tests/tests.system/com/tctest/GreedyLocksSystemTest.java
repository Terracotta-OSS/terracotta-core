/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class GreedyLocksSystemTest extends TransparentTestBase {

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(3).setApplicationInstancePerClientCount(GreedyLocksSystemTestApp.EXECUTION_COUNT)
        .setIntensity(GreedyLocksSystemTestApp.ITERATION_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return GreedyLocksSystemTestApp.class;
  }

}
