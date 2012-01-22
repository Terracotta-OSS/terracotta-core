/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

/**
 * Bounded Counter is a counter that has bounds, duh ... The value never goes beyond the bound and any set which falls
 * beyond that rang will be conveniently ignored.
 */
public class BoundedCounter extends CounterImpl {

  private final long minBound, maxBound;

  public BoundedCounter() {
    this(0L);
  }

  public BoundedCounter(long initialValue) {
    this(initialValue, Long.MIN_VALUE, Long.MAX_VALUE);
  }

  public BoundedCounter(long initialValue, long minValue, long maxValue) {
    super(initialValue);
    this.minBound = Math.min(minValue, maxValue);
    this.maxBound = Math.max(minValue, maxValue);
    if (initialValue < minBound) {
      super.setValue(minBound);
    } else if (initialValue > maxBound) {
      super.setValue(maxBound);
    }
  }

  public synchronized long decrement() {
    long current = getValue();
    if (current <= minBound) { return current; }
    return super.decrement();
  }

  public synchronized long decrement(long amount) {
    long current = getValue();
    if (current - amount <= minBound) {
      super.setValue(minBound);
      return minBound;
    }
    return super.decrement(amount);
  }

  public synchronized long getAndSet(long newValue) {
    if (newValue < minBound) {
      newValue = minBound;
    } else if (newValue > maxBound) {
      newValue = maxBound;
    }
    return super.getAndSet(newValue);
  }

  public synchronized long increment() {
    long current = getValue();
    if (current >= maxBound) { return current; }
    return super.increment();
  }

  public synchronized long increment(long amount) {
    long current = getValue();
    if (current + amount >= maxBound) {
      super.setValue(maxBound);
      return maxBound;
    }
    return super.increment(amount);
  }

  public synchronized void setValue(long newValue) {
    if (newValue < minBound) {
      newValue = minBound;
    } else if (newValue > maxBound) {
      newValue = maxBound;
    }
    super.setValue(newValue);
  }

}
