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

    public TimeStampedCounterValue getMostRecentSample() {
      return null;
    }

    public void shutdown() {
      //
    }

    public long decrement() {
      return 0;
    }

    public long decrement(long amount) {
      return 0;
    }

    public long getAndSet(long newValue) {
      return 0;
    }

    public long getValue() {
      return 0;
    }

    public long increment() {
      return 0;
    }

    public long increment(long amount) {
      return 0;
    }

    public void setValue(long newValue) {
      //
    }

    public long getAndReset() {
      return 0;
    }
  };
  
}
