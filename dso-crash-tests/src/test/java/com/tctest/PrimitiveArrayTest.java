/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.restart.RestartTestHelper;
import com.tc.util.runtime.Os;

public class PrimitiveArrayTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 16; // MUST BE 16 (8 eight primitive

  // types, and 8 wrappers)

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected long getRestartInterval(RestartTestHelper helper) {
    if (Os.isSolaris()) {
      return super.getRestartInterval(helper) * 3;
    } else {
      return super.getRestartInterval(helper);
    }
  }

  @Override
  protected Class getApplicationClass() {
    return PrimitiveArrayTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

}
