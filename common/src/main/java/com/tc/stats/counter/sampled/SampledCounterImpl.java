/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
      @Override
      public void run() {
        recordSample();
      }
    };
    
    recordSample();
  }

  @Override
  public TimeStampedCounterValue getMostRecentSample() {
    return mostRecentSample;
  }

  @Override
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

  @Override
  public long getAndReset() {
    return getAndSet(0L);
  }
}
