/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
