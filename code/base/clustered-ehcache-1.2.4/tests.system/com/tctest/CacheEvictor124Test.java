/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class CacheEvictor124Test extends TransparentTestBase {
  public static final int NODE_COUNT = 2;

  public CacheEvictor124Test() {
    //
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return CacheEvictor124TestApp.class;
  }
}
