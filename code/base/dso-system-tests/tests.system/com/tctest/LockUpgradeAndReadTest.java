/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

/**
 * This test is to make sure that the AssertionError described in DEV-587 will not be thrown
 *
 */
public class LockUpgradeAndReadTest extends TransparentTestBase {

  private static final int NODE_COUNT   = 2;
  private static final int THREAD_COUNT = 1;
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREAD_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LockUpgradeAndReadTestApp.class;
  }

}
