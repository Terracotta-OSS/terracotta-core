/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.Counter;

/**
 * Configuration for any given timed counter
 */
public class SampledCumulativeCounterConfig extends SampledCounterConfig {

  public SampledCumulativeCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample, long initialValue) {
    super(intervalSecs, historySize, isResetOnSample, initialValue);
  }

  public Counter createCounter() {
    return new SampledCumulativeCounterImpl(this);
  }
}