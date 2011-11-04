/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;

public class ShadowRootTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public ShadowRootTest() {
    // DEV-641
    disableTest();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return ShadowRootTestApp.class;
  }
}
