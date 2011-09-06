/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled.derived;

import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounterConfig;

public class SampledRateCounterConfig extends SampledCounterConfig {

  private final long initialNumeratorValue;
  private final long initialDenominatorValue;

  public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample) {
    this(intervalSecs, historySize, isResetOnSample, 0, 0);
  }

  public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample,
                                  long initialNumeratorValue, long initialDenominatorValue) {
    super(intervalSecs, historySize, isResetOnSample, 0);
    this.initialNumeratorValue = initialNumeratorValue;
    this.initialDenominatorValue = initialDenominatorValue;
  }

  @Override
  public Counter createCounter() {
    SampledRateCounterImpl sampledRateCounter = new SampledRateCounterImpl(this);
    sampledRateCounter.setValue(initialNumeratorValue, initialDenominatorValue);
    return sampledRateCounter;
  }

}
