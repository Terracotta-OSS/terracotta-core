/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import java.util.Date;

public class TimeBombTest extends TCTestCase {
  public TimeBombTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void testShouldNotRun() {
    fail("this test should not run");
  }
}
