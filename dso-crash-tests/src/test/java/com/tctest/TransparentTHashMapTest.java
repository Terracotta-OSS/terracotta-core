/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.runtime.Os;

import java.util.Date;

public class TransparentTHashMapTest extends TransparentTestBase {

  private static final int NODE_COUNT = 6;

  public TransparentTHashMapTest() {
    if (Os.isSolaris()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  @Override
  protected Class getApplicationClass() {
    return TransparentTHashMapTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

}
