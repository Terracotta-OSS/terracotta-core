/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ReentrantReadWriteLockTryLockTest extends TransparentTestBase {

  @Override
  protected Class getApplicationClass() {
    return ReentrantReadWriteLockTryLockTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(ReentrantReadWriteLockTryLockTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}