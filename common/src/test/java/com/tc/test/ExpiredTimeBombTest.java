/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(TCExtension.class)
@DisabledUntil
public class ExpiredTimeBombTest /*implements TestExecutionExceptionHandler*/ {

//  /*
//   * Timebombs don't fire on weekends since INT-1173 was fixed.
//   */
//  private boolean isWeekend() {
//    Calendar rightNow = Calendar.getInstance();
//    int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
//    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) { return true; }
//    return false;
//  }

  @Test
  public void testShouldFail() {
    fail("this test should not run");
  }

//  @Override
//  public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
//    if(isWeekend()) {
//      fail("Timebombs don't fire on weekends");
//    }
//    else if (throwable instanceof IllegalStateException && ((IllegalStateException)throwable).getMessage().contains("Timebomb has expired on")) {
//      // this is expected
//      return;
//    }
//
//    throw throwable;
//  }
}
