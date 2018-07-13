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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * TimeoutTimerConfigAnnotationTest
 */
@Disabled
@ExtendWith(TCExtension.class)
@TimeoutTimerConfig(timeoutThresholdInMillis = 870000)
public class TimeoutTimerConfigAnnotationTest {

  @Test
  public void testSuccess() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Assertions.fail("Timeout timer thread should not interrupt this test as it's execution takes shorter period than the threshold set using TimeoutTimerConfig annotation.");
    }
  }

  @Test
  public void testTimeoutInterrupt() {
    try {
      Thread.sleep(120000);

      Assertions.fail("Timeout timer thread must interrupt this test as it's execution takes longer than the threshold set using TimeoutTimerConfig annotation.");
    } catch (InterruptedException e) {
      // This is expected
    }
  }
}
