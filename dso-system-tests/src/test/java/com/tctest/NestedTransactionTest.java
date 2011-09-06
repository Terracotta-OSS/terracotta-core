/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


public class NestedTransactionTest  extends TransparentTestBase implements TestConfigurator {


  protected Class getApplicationClass() {
    return NestedTransactionApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NestedTransactionApp.NODE_COUNT).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

}
