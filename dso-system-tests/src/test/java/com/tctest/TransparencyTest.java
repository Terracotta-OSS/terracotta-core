/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


public class TransparencyTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 4;

  protected Class getApplicationClass() {
    return TransparencyTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

}
