/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.faulting;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public abstract class DualQueueFaultBase extends TransparentTestBase {

  private static final int TIMEOUT    = 30 * 60 * 1000; // 30min;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(nodeCount());
    t.getTransparentAppConfig().setIntensity(1);
    t.initializeTestRunner();
    t.getRunnerConfig().setExecutionTimeout(TIMEOUT);
  }

  protected Class getApplicationClass() {
    return DualQueueFaultTestApp.class;
  }

  protected abstract int nodeCount();
}
