/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import java.util.concurrent.atomic.AtomicLong;

public class SampledCumulativeCounterImpl extends SampledCounterImpl implements SampledCumulativeCounter {

  private AtomicLong cumulativeCount;

  public SampledCumulativeCounterImpl(SampledCounterConfig config) {
    super(config);
  }

  @Override
  protected void init() {
    super.init();
    cumulativeCount = new AtomicLong();
  }

  public long getCumulativeValue() {
    if (resetOnSample) {
      return cumulativeCount.get();
    } else {
      return getValue();
    }
  }

  @Override
  protected long getAndResetIfNecessary() {
    final long sample;
    if (resetOnSample) {
      sample = getAndReset();
      cumulativeCount.addAndGet(sample);
    } else {
      sample = getValue();
    }
    return sample;
  }

}
