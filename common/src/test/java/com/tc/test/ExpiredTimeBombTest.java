/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.test;

import java.util.Calendar;

public class ExpiredTimeBombTest extends TCTestCase {

  public ExpiredTimeBombTest() {
    timebombTest("2007-01-01");
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
