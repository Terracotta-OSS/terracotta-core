/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.object.locks;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class CompetingLockAndTryLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return CompetingLockAndTryLockTestApp.class;
  }

}
