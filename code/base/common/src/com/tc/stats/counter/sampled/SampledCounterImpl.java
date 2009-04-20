/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.CounterImpl;
import com.tc.util.concurrent.CircularLossyQueue;

import java.util.TimerTask;

/**
 * A counter that keeps sampled values
 */
public class SampledCounterImpl extends CounterImpl implements SampledCounter {
  protected final CircularLossyQueue<TimeStampedCounterValue> history;
  protected final boolean                                     resetOnSample;
  private final TimerTask                                     samplerTask;
  private final long                                          intervalMillis;

  public SampledCounterImpl(SampledCounterConfig config) {
    super(config.getInitialValue());

    this.intervalMillis = config.getIntervalSecs() * 1000;
    this.history = new CircularLossyQueue<TimeStampedCounterValue>(config.getHistorySize());
    this.resetOnSample = config.isResetOnSample();

    this.samplerTask = new TimerTask() {
      public void run() {
        recordSample();
      }
    };

    recordSample();
  }

  public TimeStampedCounterValue getMostRecentSample() {
    return this.history.peek();
  }

  public TimeStampedCounterValue[] getAllSampleValues() {
    return this.history.toArray(new TimeStampedCounterValue[this.history.depth()]);
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
    TimeStampedCounterValue timedSample = new TimeStampedCounterValue(now, sample);

    history.push(timedSample);
  }

  public long getAndReset() {
    return getAndSet(0L);
  }
}
