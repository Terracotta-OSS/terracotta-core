/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
