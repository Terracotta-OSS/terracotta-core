/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


public class MassCloneTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public MassCloneTest() {
    //
  }
  
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return MassCloneTestApp.class;
  }
}
