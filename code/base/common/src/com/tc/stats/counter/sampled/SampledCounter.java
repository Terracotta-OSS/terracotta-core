/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.Counter;


public interface SampledCounter extends Counter {
  
  void shutdown();
  
  TimeStampedCounterValue getMostRecentSample();

  TimeStampedCounterValue[] getAllSampleValues();

  TimeStampedCounterValue getMin();

  TimeStampedCounterValue getMax();

  double getAverage();

  
}
