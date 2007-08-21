package com.tctest;

public class EhcacheManagerTest extends TransparentTestBase {

  public EhcacheManagerTest() {
     disableAllUntil("2007-08-30");
  }
  
  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(
        EhcacheManagerTestApp.EXPECTED_THREAD_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return EhcacheManagerTestApp.class;
  }
  
}

