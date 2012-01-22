/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation
 */
public class CounterImpl implements Counter, Serializable {
  private AtomicLong value;

  public CounterImpl() {
    this(0L);
  }

  public CounterImpl(long initialValue) {
    this.value = new AtomicLong(initialValue);
  }

  public long increment() {
    return value.incrementAndGet();
  }

  public long decrement() {
    return value.decrementAndGet();
  }

  public long getAndSet(long newValue) {
    return value.getAndSet(newValue);
  }

  public long getValue() {
    return value.get();
  }

  public long increment(long amount) {
    return value.addAndGet(amount);
  }

  public long decrement(long amount) {
    return value.addAndGet(amount * -1);
  }

  public void setValue(long newValue) {
    value.set(newValue);
  }

}
