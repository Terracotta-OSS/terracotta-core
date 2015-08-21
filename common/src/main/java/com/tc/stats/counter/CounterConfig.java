/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

public interface CounterConfig {

  Counter createCounter();
  
  long getInitialValue();

}
