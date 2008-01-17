/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.counter;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.util.TCTimerImpl;

import java.util.Timer;

public class CounterManagerImpl implements CounterManager {
  private final Timer timer    = new TCTimerImpl("SampledCounterManager Timer", true);
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

  public synchronized Counter createCounter(Object config) {
    if (shutdown) { throw new IllegalStateException("counter manager is shutdown"); }
    if (config == null) { throw new NullPointerException("config cannot be null"); }

    if (config instanceof CounterConfig) {
      CounterConfig cc = (CounterConfig) config;
      Counter counter = new CounterImpl(cc.getInitialValue());
      return counter;
    } else if (config instanceof BoundedCounterConfig) {
      BoundedCounterConfig bcc = (BoundedCounterConfig) config;
      Counter counter = new BoundedCounter(bcc.getInitialValue(), bcc.getMinBound(), bcc.getMaxBound());
      return counter;
    } else if (config instanceof SampledCounterConfig) {
      SampledCounterConfig scc = (SampledCounterConfig) config;
      SampledCounterImpl counter;
      counter = new SampledCounterImpl(scc);

      final long period = scc.getIntervalSecs() * 1000;
      timer.schedule(counter.getTimerTask(), period, period);
      return counter;
    } else {
      throw new AssertionError("Wrong config : " + config);
    }

  }

  public void shutdownCounter(Counter counter) {
    if (counter instanceof SampledCounter) {
      SampledCounter sc = (SampledCounter) counter;
      sc.shutdown();
    }
  }

}
