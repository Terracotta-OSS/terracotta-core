/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;

public final class CounterConfig {

  private final long initialValue;

  public CounterConfig(long initialValue) {
    this.initialValue = initialValue;
  }
  
  public long getInitialValue() {
    return initialValue;
  }
}
