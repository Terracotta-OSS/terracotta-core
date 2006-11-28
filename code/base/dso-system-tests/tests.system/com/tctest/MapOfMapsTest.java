/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class MapOfMapsTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 2;

  protected Class getApplicationClass() {
    return MapOfMapsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

}
