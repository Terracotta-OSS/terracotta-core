/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.Counter;


public interface SampledCounter extends Counter {
  
  void shutdown();
  
  TimeStampedCounterValue getMostRecentSample();

  long getAndReset();
  
  public final static SampledCounter NULL_SAMPLED_COUNTER = new SampledCounter() {

    @Override
    public TimeStampedCounterValue getMostRecentSample() {
      return null;
    }

    @Override
    public void shutdown() {
      //
    }

    @Override
    public long decrement() {
      return 0;
    }

    @Override
    public long decrement(long amount) {
      return 0;
    }

    @Override
    public long getAndSet(long newValue) {
      return 0;
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public long increment() {
      return 0;
    }

    @Override
    public long increment(long amount) {
      return 0;
    }

    @Override
    public void setValue(long newValue) {
      //
    }

    @Override
    public long getAndReset() {
      return 0;
    }
  };
  
}
