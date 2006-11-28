/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.timedtask;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.performance.timedtask.TimedChangeReplicationTestApp;

public abstract class TimedChangeReplicationBase extends TransparentTestBase {

  private static final int TIMEOUT    = 30 * 60 * 1000; // 30min;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(nodeCount());
    t.initializeTestRunner();
    t.getRunnerConfig().executionTimeout = TIMEOUT;
  }

  protected Class getApplicationClass() {
    return TimedChangeReplicationTestApp.class;
  }
  
  protected abstract int nodeCount();
}
