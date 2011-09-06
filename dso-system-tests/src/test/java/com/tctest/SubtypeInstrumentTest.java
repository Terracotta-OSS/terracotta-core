/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


public class SubtypeInstrumentTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public SubtypeInstrumentTest() {
    // this.disableAllUntil("2008-05-20");
  }

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SubtypeInstrumentTestApp.class;
  }
}

