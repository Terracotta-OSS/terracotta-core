/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.Date;

public class LinkedBlockingQueueInterruptTakeOOOTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public LinkedBlockingQueueInterruptTakeOOOTest() {
    // MNK-527
    if (Os.isSolaris() && !Vm.isJDK16Compliant()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueInterruptTakeTestApp.class;
  }

}