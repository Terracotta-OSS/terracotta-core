/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import java.util.concurrent.atomic.AtomicLong;

public class SampledCumulativeCounterImpl extends SampledCounterImpl implements SampledCumulativeCounter {

  private AtomicLong cumulativeCount;

  public SampledCumulativeCounterImpl(SampledCounterConfig config) {
    super(config);
    cumulativeCount = new AtomicLong(config.getInitialValue());
  }

  public long getCumulativeValue() {
    if (resetOnSample) {
      return cumulativeCount.get();
    } else {
      return getValue();
    }
  }

  @Override
  public long decrement() {
    cumulativeCount.decrementAndGet();
    return super.decrement();
  }

  @Override
  public long decrement(long amount) {
    cumulativeCount.addAndGet(amount * -1);
    return super.decrement(amount);
  }

  @Override
  public long increment() {
    cumulativeCount.incrementAndGet();
    return super.increment();
  }

  @Override
  public long increment(long amount) {
    cumulativeCount.addAndGet(amount);
    return super.increment(amount);
  }

}
