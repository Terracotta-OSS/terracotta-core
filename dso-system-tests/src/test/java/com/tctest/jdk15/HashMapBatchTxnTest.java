/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

/*
 * Test case for CDV-253
 */

public class HashMapBatchTxnTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public HashMapBatchTxnTest() {
    // MNK-362
    if (Vm.isIBM()) {
      //disableAllUntil("2007-12-04");
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return HashMapBatchTxnTestApp.class;
  }

}
