/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.CounterImpl;

import java.util.TimerTask;

/**
 * A counter that keeps sampled values
 */
public class SampledCounterImpl extends CounterImpl implements SampledCounter {
  protected final boolean                                     resetOnSample;
  private final TimerTask                                     samplerTask;
  private final long                                          intervalMillis;
  private volatile TimeStampedCounterValue                    mostRecentSample;
  
  public SampledCounterImpl(SampledCounterConfig config) {
    super(config.getInitialValue());

    this.intervalMillis = config.getIntervalSecs() * 1000;
    this.resetOnSample = config.isResetOnSample();

    this.samplerTask = new TimerTask() {
      public void run() {
        recordSample();
      }
    };
    
    recordSample();
  }

  public TimeStampedCounterValue getMostRecentSample() {
    return mostRecentSample;
  }

  public void shutdown() {
    if (samplerTask != null) {
      samplerTask.cancel();
    }
  }

  public TimerTask getTimerTask() {
    return this.samplerTask;
  }

  public long getIntervalMillis() {
    return intervalMillis;
  }

  void recordSample() {
    final long sample;
    if (resetOnSample) {
      sample = getAndReset();
    } else {
      sample = getValue();
    }

    final long now = System.currentTimeMillis();
    mostRecentSample = new TimeStampedCounterValue(now, sample);
  }

  public long getAndReset() {
    return getAndSet(0L);
  }
}
