/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class TreeMapBuggyComparatorTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return TreeMapBuggyComparatorTestApp.class;
  }

}
