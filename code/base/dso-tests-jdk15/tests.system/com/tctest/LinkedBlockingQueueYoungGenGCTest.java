/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class LinkedBlockingQueueYoungGenGCTest extends YoungGCTestBase implements TestConfigurator {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(LinkedBlockingQueueTestApp.GC_TEST_KEY, "true");
    super.doSetUp(t);
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueTestApp.class;
  }
}
