/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.counter.sampled;

public class TestSampledCounterManager implements SampledCounterManager {

  public TestSampledCounterManager() {
    super();
  }

  public SampledCounter createCounter(SampledCounterConfig config) {
    return new SampledCounterImpl(config);
  }

  public void shutdown() {
    //
  }

}
