/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

/*
 * Unit test for measuring the overhead of the instrumented Field class. For correctness
 * tests for instrumented Field class, refer to the ReflectionFieldTest.
 */
public class BigIntegerTest extends TransparentTestBase {
  private final static int NODE_COUNT = 1;
  private final static int LOOP_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BigIntegerTestApp.class;
  }
 
}
