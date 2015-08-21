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

  @Override
  public long increment() {
    return value.incrementAndGet();
  }

  @Override
  public long decrement() {
    return value.decrementAndGet();
  }

  @Override
  public long getAndSet(long newValue) {
    return value.getAndSet(newValue);
  }

  @Override
  public long getValue() {
    return value.get();
  }

  @Override
  public long increment(long amount) {
    return value.addAndGet(amount);
  }

  @Override
  public long decrement(long amount) {
    return value.addAndGet(amount * -1);
  }

  @Override
  public void setValue(long newValue) {
    value.set(newValue);
  }

}
