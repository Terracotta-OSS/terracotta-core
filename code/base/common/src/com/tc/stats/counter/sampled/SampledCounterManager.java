/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.counter.sampled;


public interface SampledCounterManager {  
  SampledCounter createCounter(SampledCounterConfig config);
  
  void shutdown();  
}
