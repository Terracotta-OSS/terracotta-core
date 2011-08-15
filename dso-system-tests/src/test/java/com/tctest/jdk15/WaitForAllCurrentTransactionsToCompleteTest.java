/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

public class WaitForAllCurrentTransactionsToCompleteTest extends TransparentTestBase {

  private static final int NODE_COUNT = 4;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    int adminPort = getAdminPort();
    cfg.setAttribute(WaitForAllCurrentTransactionsToCompleteTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  protected Class getApplicationClass() {
    return WaitForAllCurrentTransactionsToCompleteTestApp.class;
  }

}
