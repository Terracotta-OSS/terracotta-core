package com.tctest;

import java.util.Date;

public class EhcacheEviction13Test extends TransparentTestBase {
  public EhcacheEviction13Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(EhcacheEvictionTestApp.EXPECTED_THREAD_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return EhcacheEvictionTestApp.class;
  }
}
