/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

public interface SampledCumulativeCounter extends SampledCounter {
  
  public long getCumulativeValue();

}
