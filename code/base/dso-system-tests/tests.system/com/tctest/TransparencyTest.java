/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
