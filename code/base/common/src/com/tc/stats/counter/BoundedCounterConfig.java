/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.counter;

public class BoundedCounterConfig {

  private final long initialValue;
  private final long min;
  private final long max;

  public BoundedCounterConfig(long initialValue, long min, long max) {
    this.initialValue = initialValue;
    this.min = min;
    this.max = max;
  }

  public long getInitialValue() {
    return initialValue;
  }

  public long getMinBound() {
    return min;
  }

  public long getMaxBound() {
    return max;
  }
}
