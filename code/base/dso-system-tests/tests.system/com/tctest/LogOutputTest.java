/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class LogOutputTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

  public LogOutputTest() {
    disableAllUntil("2007-06-15");
  }

  protected Class getApplicationClass() {
    return LogOutputTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }
}
