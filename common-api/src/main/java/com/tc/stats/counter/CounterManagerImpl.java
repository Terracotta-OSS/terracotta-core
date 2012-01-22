/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterImpl;

import java.util.Timer;

public class CounterManagerImpl implements CounterManager {
  private final Timer timer    = new Timer("SampledCounterManager Timer", true);
  private boolean     shutdown = false;

  public CounterManagerImpl() {
    super();
  }

  public synchronized void shutdown() {
    if (shutdown) { return; }
    try {
      timer.cancel();
    } finally {
      shutdown = true;
    }
  }

  public synchronized Counter createCounter(CounterConfig config) {
    if (shutdown) { throw new IllegalStateException("counter manager is shutdown"); }
    if (config == null) { throw new NullPointerException("config cannot be null"); }

    Counter counter = config.createCounter();
    if (counter instanceof SampledCounterImpl) {
      SampledCounterImpl sampledCounter = (SampledCounterImpl) counter;
      timer.schedule(sampledCounter.getTimerTask(), sampledCounter.getIntervalMillis(), sampledCounter
          .getIntervalMillis());
    }
    return counter;

  }

  public void shutdownCounter(Counter counter) {
    if (counter instanceof SampledCounter) {
      SampledCounter sc = (SampledCounter) counter;
      sc.shutdown();
    }
  }

}
