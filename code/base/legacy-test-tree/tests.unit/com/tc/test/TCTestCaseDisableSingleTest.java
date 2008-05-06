/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

/**
 * Test that single test cases can be disabled
 */
public class TCTestCaseDisableSingleTest extends TCTestCase {
  static final int numTests    = 3;
  static int       count       = 0;
  static boolean   testSkipped = false;

  public TCTestCaseDisableSingleTest() {
    disableTestUntil("testSkip1", "3000-01-01");
    disableTestUntil("testSkip2", "3000-01-01");
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    assertTrue("count is " + count, (count > 0) && (count <= numTests));

    if (count == numTests) {
      if (!testSkipped) {
        fail("Test not skipped");
      }
    }
  }

  public void testNotSkipped() {
    testSkipped = true;
    return;
  }

  public void testSkip1() {
    throw new RuntimeException("should have been skipped");
  }

  public void testSkip2() {
    throw new RuntimeException("should have been skipped");
  }

  public void runBare() throws Throwable {
    count++;
    super.runBare();
  }
}