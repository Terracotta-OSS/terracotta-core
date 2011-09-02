/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class ClientGCLockHeldTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 1;
  private static final int THREADS_COUNT = 1;

  protected Class getApplicationClass() {
    return ClientGCLockHeldApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }


}
 