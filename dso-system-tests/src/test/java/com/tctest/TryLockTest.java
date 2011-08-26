/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class TryLockTest extends TransparentTestBase {
  @Override
  protected Class getApplicationClass() {
    return TryLockTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(TryLockTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}