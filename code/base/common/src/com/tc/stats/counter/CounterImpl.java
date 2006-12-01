/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;

/**
 * A simple counter implementation
 */
public class CounterImpl implements Counter {
  private long value;
  private long min;
  private long max;

  public CounterImpl() {
    this(0L);
  }

  public CounterImpl(long initialValue) {
    this.value = initialValue;
    this.min = initialValue;
    this.max = initialValue;
  }

  public synchronized long increment() {
    final long newValue = ++this.value;
    setValue(newValue);
    return newValue;
  }

  public synchronized long decrement() {
    final long newValue = --this.value;
    setValue(newValue);
    return newValue;
  }

  public synchronized long getAndSet(long newValue) {
    final long previousValue = this.value;
    setValue(newValue);
    return previousValue;
  }

  public synchronized long getValue() {
    return this.value;
  }

  public synchronized long getMaxValue() {
    return this.max;
  }

  public synchronized long getMinValue() {
    return this.min;
  }

  public synchronized long increment(long amount) {
    final long newValue = this.value += amount;
    setValue(newValue);
    return newValue;
  }

  public synchronized long decrement(long amount) {
    final long newValue = this.value -= amount;
    setValue(newValue);
    return newValue;
  }

  public synchronized void setValue(long newValue) {
    if (newValue > this.max) {
      this.max = newValue;
    }

    if (newValue < this.min) {
      this.min = newValue;
    }

    this.value = newValue;
  }

}
