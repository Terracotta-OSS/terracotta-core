/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;

public class CounterConfig {

  private final long initialValue;

  public CounterConfig(long initialValue) {
    this.initialValue = initialValue;
  }
  
  public final long getInitialValue() {
    return initialValue;
  }

  public Counter createCounter() {
    return new CounterImpl(initialValue);
  }
}
