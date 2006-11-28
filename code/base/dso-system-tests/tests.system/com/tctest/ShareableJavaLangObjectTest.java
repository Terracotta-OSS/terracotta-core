/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

/**
 * TODO Jun 27, 2005: I, teck, am too lazy to write a single sentence describing
 * what this class is for.
 */
public class ShareableJavaLangObjectTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ShareableJavaLangObjectTestApp.class;
  }

}
