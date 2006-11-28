/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class ClassInMapTest extends TransparentTestBase {

  public ClassInMapTest() {
    super();
  }

  private static final int NODE_COUNT = 2;

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }


  protected Class getApplicationClass() {
    return ClassInMapTestApp.class;
  }

}
