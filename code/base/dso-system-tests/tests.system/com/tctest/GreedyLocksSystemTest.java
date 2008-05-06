/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
