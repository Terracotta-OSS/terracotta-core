/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 * 
 * @author hhuynh
 */
package com.tctest;

public class NotShareableReadWriteLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public NotShareableReadWriteLockTest() {
    // DEV-1209
    disableAllUntil("2008-06-01");
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return NotShareableReadWriteLockTestApp.class;
  }

}
