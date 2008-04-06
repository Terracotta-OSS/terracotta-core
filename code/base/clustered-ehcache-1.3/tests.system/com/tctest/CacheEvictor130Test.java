package com.tctest;

import java.util.Date;

public class CacheEvictor130Test extends TransparentTestBase {
  public static final int NODE_COUNT = 2;

  public CacheEvictor130Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return CacheEvictor130TestApp.class;
  }
}
