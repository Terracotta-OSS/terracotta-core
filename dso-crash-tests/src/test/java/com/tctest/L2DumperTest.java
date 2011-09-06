/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


public class L2DumperTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

  @Override
  protected Class getApplicationClass() {
    return L2DumperTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }
}