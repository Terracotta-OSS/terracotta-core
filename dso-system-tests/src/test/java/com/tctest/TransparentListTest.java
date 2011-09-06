/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


public class TransparentListTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 3;

  protected Class getApplicationClass() {
    return TransparentListApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

}
