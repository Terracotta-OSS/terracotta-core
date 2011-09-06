/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

/*
 * Unit test for testing method invocations for BigInteger and BigDecimal classes.
 * Test for testing the sharing of BigInteger and BigDecimal objects is handled by
 * TransparentTest.
 */
public class BigIntegerDecimalTest extends TransparentTestBase {
  private final static int NODE_COUNT = 1;
  private final static int LOOP_COUNT = 1;
  
  public void setUp() throws Exception {
    super.setUp();
    
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BigIntegerDecimalTestApp.class;
  }
 
}
