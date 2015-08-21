/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;



public interface CounterManager {  
  Counter createCounter(CounterConfig config);
  
  void shutdown();

  void shutdownCounter(Counter counter);  
}
