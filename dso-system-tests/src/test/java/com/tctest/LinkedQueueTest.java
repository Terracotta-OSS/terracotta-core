/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class LinkedQueueTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 3;

  protected Class getApplicationClass() {
    return LinkedQueueTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setApplicationInstancePerClientCount(2).setIntensity(1);
    t.initializeTestRunner();

  }

}
