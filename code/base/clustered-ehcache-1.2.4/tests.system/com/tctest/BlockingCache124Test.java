/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import java.util.Date;

public class BlockingCache124Test extends TransparentTestBase {
  public static final int NODE_COUNT = 3;

  public BlockingCache124Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }
  
  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BlockingCache124TestApp.class;
  }
}
