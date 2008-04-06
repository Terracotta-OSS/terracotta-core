package com.tctest;

import java.util.Date;

public class EhcacheEvictionTest extends TransparentTestBase {
  public EhcacheEvictionTest() {
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
