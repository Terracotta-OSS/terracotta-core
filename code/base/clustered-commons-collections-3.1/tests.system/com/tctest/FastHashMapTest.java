package com.tctest;

public final class FastHashMapTest extends TransparentTestBase {

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(FastHashMapTestApp.EXPECTED_THREAD_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return FastHashMapTestApp.class;
  }

}
