/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;


public class TimeBombTest extends TCTestCase {
  public TimeBombTest() {
    disableTest();
  }

  public void testShouldNotRun() {
    fail("this test should not run");
  }
}
