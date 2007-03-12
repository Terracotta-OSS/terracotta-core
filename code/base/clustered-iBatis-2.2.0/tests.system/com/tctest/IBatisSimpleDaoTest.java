package com.tctest;

import com.tc.util.runtime.Vm;

public class IBatisSimpleDaoTest extends TransparentTestBase {
  private final static int NODE_COUNT = 2;

  private final static int LOOP_COUNT = 1;
  
  public IBatisSimpleDaoTest() {
    if (Vm.isJDK16()) {
      disableAllUntil("2010-01-01");
    }
  }

  public void setUp() throws Exception {
    super.setUp();

    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return IBatisSimpleDaoTestApp.class;
  }
}
