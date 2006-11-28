/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
