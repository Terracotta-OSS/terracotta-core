/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

/**
 * A simple counter
 */
public interface Counter {

  long increment();

  long decrement();

  long getAndSet(long newValue);

  long getValue();

  long increment(long amount);

  long decrement(long amount);

  void setValue(long newValue);

}
