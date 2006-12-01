/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.throughput;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.performance.throughput.DualQueueThroughputTestApp;

public class DualQueueThroughput extends TransparentTestBase {

  private static final int NODE_COUNT = 1;
  private static final int TIMEOUT    = 30 * 60 * 1000; // 30min;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    t.getRunnerConfig().executionTimeout = TIMEOUT;
  }

  protected Class getApplicationClass() {
    return DualQueueThroughputTestApp.class;
  }
}
