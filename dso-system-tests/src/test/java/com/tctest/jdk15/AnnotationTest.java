/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class AnnotationTest extends TransparentTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(3).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AnnotationTestApp.class;
  }

  protected boolean canRunCrash() {
    return false;
  }

}
