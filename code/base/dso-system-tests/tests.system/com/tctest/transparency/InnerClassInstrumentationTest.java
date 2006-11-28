/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.transparency;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class InnerClassInstrumentationTest extends TransparentTestBase {

  protected Class getApplicationClass() {
    return InnerClassInstrumentationTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

}
