/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


public class AutoSynchTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public AutoSynchTest() {
    this.disableAllUntil("2007-05-19");
  }

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AutoSynchTestApp.class;
  }
}

