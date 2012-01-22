/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.Assert;

public class TimerSpecFactory {
  public TimerSpec newTimerSpec(int waitArgCount, long millis, int nanos) {
    switch (waitArgCount) {
      case 2: {
        return new TimerSpec(millis, nanos);
      }
      case 1: {
        return new TimerSpec(millis);
      }
      case 0: {
        return new TimerSpec();
      }
      default: {
        throw Assert.failure("Invalid wait argument count: " + waitArgCount);
      }
    }
  }
}
