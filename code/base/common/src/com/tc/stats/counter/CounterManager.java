/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;



public interface CounterManager {  
  Counter createCounter(CounterConfig config);
  
  void shutdown();

  void shutdownCounter(Counter counter);  
}
