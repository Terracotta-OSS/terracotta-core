/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.Counter;
import com.tc.stats.counter.SimpleCounterConfig;

/**
 * Configuration for any given timed counter
 */
public class SampledCounterConfig extends SimpleCounterConfig {
  private final int     intervalSecs;
  private final int     historySize;
  private final boolean isReset;

  /**
   * Make a new timed counter config (duh)
   * 
   * @param intervalSecs the interval (in seconds) between sampling
   * @param historySize number of counter samples that will be retained in memory
   * @param isResetOnSample true if the counter should be reset to 0 upon each sample
   */
  public SampledCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample, long initialValue) {
    super(initialValue);
    if (intervalSecs < 1) { throw new IllegalArgumentException("Interval (" + intervalSecs
                                                               + ") must be greater than or equal to 1"); }
    if (historySize < 1) { throw new IllegalArgumentException("History size (" + historySize
                                                              + ") must be greater than or equal to 1"); }

    this.intervalSecs = intervalSecs;
    this.historySize = historySize;
    this.isReset = isResetOnSample;
  }

  public int getHistorySize() {
    return historySize;
  }

  public int getIntervalSecs() {
    return intervalSecs;
  }

  public boolean isResetOnSample() {
    return this.isReset;
  }

  @Override
  public Counter createCounter() {
    return new SampledCounterImpl(this);
  }
}
