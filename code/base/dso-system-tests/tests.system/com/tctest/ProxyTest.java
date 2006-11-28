/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

/*
 * Unit test for measuring the overhead of the instrumented Field class. For correctness
 * tests for instrumented Field class, refer to the ReflectionFieldTest.
 */
public class ProxyTest extends TransparentTestBase {
  private final static int NODE_COUNT = 3;

  public void setUp() throws Exception {
    super.setUp();
    
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ProxyTestApp.class;
  }
 
}
