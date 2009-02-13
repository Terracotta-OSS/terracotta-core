/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  
  public final static SampledCounter NULL_SAMPLED_COUNTER = new SampledCounter() {

    public TimeStampedCounterValue[] getAllSampleValues() {
      return null;
    }

    public double getAverage() {
      return 0;
    }

    public TimeStampedCounterValue getMax() {
      return null;
    }

    public TimeStampedCounterValue getMin() {
      return null;
    }

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

    public long getMaxValue() {
      return 0;
    }

    public long getMinValue() {
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
  };
  
}
