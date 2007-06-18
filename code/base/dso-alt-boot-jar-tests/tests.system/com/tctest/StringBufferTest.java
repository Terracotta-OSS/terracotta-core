/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.runtime.Vm;
import com.tctest.stringbuffer.StringBufferTestApp;

import java.util.Date;

public class StringBufferTest extends TransparentTestBase {

  private static final int NODE_COUNT = 5;
  
  public StringBufferTest() {
    if  (Vm.isIBM()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return StringBufferTestApp.class;
  }

}
