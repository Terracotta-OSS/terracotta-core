package com.tctest;

import java.util.Date;

public class BlockingCache130Test extends TransparentTestBase {
  public static final int NODE_COUNT = 3;

  public BlockingCache130Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BlockingCache130TestApp.class;
  }
}
