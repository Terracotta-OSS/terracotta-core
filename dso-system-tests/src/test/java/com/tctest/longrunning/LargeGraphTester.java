/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.longrunning;

import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;
import com.tctest.TransparentTestBase;

public class LargeGraphTester extends TransparentTestBase implements TestConfigurator {

  int NODE_COUNT           = 4;
  int LOOP_ITERATION_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT)
    .setIntensity(LOOP_ITERATION_COUNT);

    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LargeGraphTestApp.class;
  }

}
