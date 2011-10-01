/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.restart.RestartTestHelper;
import com.tc.util.runtime.Os;

public class LinkedBlockingQueueL1ReconnectCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 8;

  public LinkedBlockingQueueL1ReconnectCrashTest() {
    disableAllUntil("2011-09-30");
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected long getRestartInterval(RestartTestHelper helper) {
    if (Os.isSolaris() || Memory.isMemoryLow()) {
      return super.getRestartInterval(helper) * 3;
    } else {
      return super.getRestartInterval(helper);
    }
  }

  @Override
  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  protected boolean enableL1Reconnec() {
    return true;
  }

}
