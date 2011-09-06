/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

public class IgnoreRewriteTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  public IgnoreRewriteTest() {
    //this.disableAllUntil("2007-05-01");
  }
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return IgnoreRewriteTestApp.class;
  }
 

}
