/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

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
