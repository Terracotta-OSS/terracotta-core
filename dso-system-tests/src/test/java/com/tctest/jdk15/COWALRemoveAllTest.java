/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;

import java.util.Date;

//test CopyOnWriteArrayList.removeAll()
public class COWALRemoveAllTest extends TransparentTestBase {
  private static final int NODE_COUNT = 3;

  public COWALRemoveAllTest() {
    if (Vm.isIBM()) {
      // these currently don't have to work on the IBM JDK
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return COWALRemoveAllTestApp.class;
  }

}
