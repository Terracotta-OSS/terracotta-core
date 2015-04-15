/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import com.tc.test.TCTestCase;

public class TimeMovesForwardTest extends TCTestCase {

  private static final long SAMPLE_DURATION = 60000;

  public void testTimeNoDelay() {
    measure(false);
  }

  public void testTimeWithDelay() {
    measure(true);
  }

  public void measure(boolean delay) {
    final long start = System.currentTimeMillis();
    long prev = start;
    int count = 0;

    while ((prev - start) < SAMPLE_DURATION) {
      count++;
      long sample = System.currentTimeMillis();
      if (sample < prev) { throw new AssertionError("Clock moved from " + prev + " to " + sample); }
      prev = sample;

      if (delay) {
        delay(sample);
      }
    }

    System.out.println(count + " samples took " + (System.currentTimeMillis() - start) + " millis");
  }

  public void delay(long sample) {
    int n = 12500;
    for (int i = 0; i < n; i++) {
      // this code should prevent the optimizer from making this method a total noop
      if (i == sample) {
        System.out.println();
      }
    }
  }

}
