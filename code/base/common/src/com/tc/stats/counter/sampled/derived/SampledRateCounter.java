/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled.derived;

import com.tc.stats.counter.sampled.SampledCounter;

public interface SampledRateCounter extends SampledCounter {

  public void increment(long numerator, long denominator);

  public void decrement(long numerator, long denominator);

  public void setValue(long numerator, long denominator);
  
  public void setNumeratorValue(long newValue);
  
  public void setDenominatorValue(long newValue);

}
