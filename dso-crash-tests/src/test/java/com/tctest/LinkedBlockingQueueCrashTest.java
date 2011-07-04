/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

//import com.tc.util.runtime.Vm;

public class LinkedBlockingQueueCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 8;
  
  public LinkedBlockingQueueCrashTest() {
    //if (Vm.isIBM()) {
    //  disableAllUntil("2007-08-30");
    //}
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

 
}
