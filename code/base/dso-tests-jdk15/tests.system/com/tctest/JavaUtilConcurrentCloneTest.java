/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class JavaUtilConcurrentCloneTest extends TransparentTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(JavaUtilConcurrentCloneTestApp.NODE_COUNT)
        .setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return JavaUtilConcurrentCloneTestApp.class;
  }

}
