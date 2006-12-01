/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.util.TCTimerImpl;

import java.util.Timer;

public class SampledCounterManagerImpl implements SampledCounterManager {
  private final Timer timer    = new TCTimerImpl("SampledCounterManager Timer", true);
  private boolean     shutdown = false;

  public SampledCounterManagerImpl() {
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

  public synchronized SampledCounter createCounter(SampledCounterConfig config) {
    if (shutdown) { throw new IllegalStateException("counter manager is shutdown"); }
    if (config == null) { throw new NullPointerException("config cannot be null"); }

    final SampledCounterImpl counter = new SampledCounterImpl(config);

    final long period = config.getIntervalSecs() * 1000;
    timer.schedule(counter.getTimerTask(), period, period);

    return counter;
  }

}
