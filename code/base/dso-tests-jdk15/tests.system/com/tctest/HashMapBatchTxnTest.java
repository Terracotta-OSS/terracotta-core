/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

/*
 * Test case for CDV-253
 */

public class HashMapBatchTxnTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  public HashMapBatchTxnTest() {
    disableAllUntil("2007-08-15");
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return HashMapBatchTxnTestApp.class;
  }
 
}
