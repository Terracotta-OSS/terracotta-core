/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class AtomicIntegerTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AtomicIntegerTestApp.class;
  }

}
