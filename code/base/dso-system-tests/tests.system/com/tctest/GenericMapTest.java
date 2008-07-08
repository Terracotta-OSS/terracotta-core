/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class GenericMapTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public GenericMapTest() {
    disableAllUntil("2008-07-15");
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return GenericMapTestApp.class;
  }

}
