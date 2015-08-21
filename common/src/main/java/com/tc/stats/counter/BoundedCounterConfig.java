/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

public class BoundedCounterConfig extends SimpleCounterConfig {

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

  @Override
  public Counter createCounter() {
    return new BoundedCounter(getInitialValue(), min, max);
  }
}
