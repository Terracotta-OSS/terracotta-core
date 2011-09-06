/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
