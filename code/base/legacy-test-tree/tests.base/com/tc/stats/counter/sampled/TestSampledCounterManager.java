/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
