/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;


public class NestedReadOnlyTransactionTest  extends TransparentTestBase implements TestConfigurator {
  private final static int     NODE_COUNT              = 2;
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return NestedReadOnlyTransactionTestApp.class;
  }
}
