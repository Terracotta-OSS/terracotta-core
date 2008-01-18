/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.counter;

public class BoundedCounterConfig extends CounterConfig {

  private final long min;
  private final long max;

  public BoundedCounterConfig(long initialValue, long min, long max) {
    super(initialValue);
    this.min = min;
    this.max = max;
  }

  public long getMinBound() {
    return min;
  }

  public long getMaxBound() {
    return max;
  }

  public Counter createCounter() {
    return new BoundedCounter(getInitialValue(), min, max);
  }
}
