/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import java.util.Calendar;

public class ExpiredTimeBombTest extends TCTestCase {

  public ExpiredTimeBombTest() {
    disableAllUntil("2007-01-01");
  }

  /*
   * Timebombs don't fire on weekends since INT-1173 was fixed.
   */
  private boolean isWeekend() {
    Calendar rightNow = Calendar.getInstance();
    int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) { return true; }
    return false;
  }

  public void testShouldFail() {
    if (!isWeekend()) {
      fail("this test should not run");
    }
  }

  @Override
  public void runBare() throws Throwable {
    try {
      super.runBare();
    } catch (Throwable t) {
      // expected
      return;
    }
    if (!isWeekend()) {
      fail("should have thrown exception when timebomb expired");
    }
  }
}
