/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class TransparentVectorTest extends TransparentTestBase {
  private static final int NODE_COUNT           = 3;
  private static final int EXECUTION_COUNT      = 3;
  private static final int LOOP_ITERATION_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TransparentVectorTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
