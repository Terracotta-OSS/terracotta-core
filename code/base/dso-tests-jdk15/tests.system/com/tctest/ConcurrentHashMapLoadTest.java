/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.util.runtime.Vm;

public class ConcurrentHashMapLoadTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public ConcurrentHashMapLoadTest() {
    if (Vm.isJDK16()) {
      disableAllUntil("2007-10-30");
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ConcurrentHashMapLoadTestApp.class;
  }

}
