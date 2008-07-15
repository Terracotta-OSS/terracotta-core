/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;


public class ExpiredTimeBombTest extends TCTestCase {

  public ExpiredTimeBombTest() {
    disableAllUntil("2007-01-01");
  }

  public void testShouldFail() {
    fail("this test should not run");
  }
  
  public void runBare() throws Throwable {
    try {
      super.runBare();
    } catch (Throwable t) {
      // expected
      return;
    }
    fail("should have thrown exception when timebomb expired");
  }
}
