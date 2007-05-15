package com.tctest;

public final class LRUMapTest extends TransparentTestBase {

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(LRUMapTestApp.EXPECTED_THREAD_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LRUMapTestApp.class;
  }

}
