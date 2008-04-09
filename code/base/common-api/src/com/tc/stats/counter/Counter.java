/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  long getMaxValue();

  long getMinValue();

  long increment(long amount);

  long decrement(long amount);

  void setValue(long newValue);

}
