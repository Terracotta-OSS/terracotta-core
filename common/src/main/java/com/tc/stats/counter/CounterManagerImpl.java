/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.stats.counter;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterImpl;

import java.util.Timer;

public class CounterManagerImpl implements CounterManager {
  private Timer timer    = null;
  private boolean     shutdown = false;

  public CounterManagerImpl() {
    super();
  }

  @Override
  public synchronized void shutdown() {
    if (shutdown) { return; }
    try {
      if (timer != null) {
        timer.cancel();
      }
    } finally {
      shutdown = true;
    }
  }
  
  @Override
  public synchronized Counter createCounter(CounterConfig config) {
    if (shutdown) { throw new IllegalStateException("counter manager is shutdown"); }
    if (config == null) { throw new NullPointerException("config cannot be null"); }

    Counter counter = config.createCounter();
    if (counter instanceof SampledCounterImpl) {
      SampledCounterImpl sampledCounter = (SampledCounterImpl) counter;
      if (timer == null) {
        timer = new Timer("SampledCounterManager Timer", true);
      }
      timer.schedule(sampledCounter.getTimerTask(), sampledCounter.getIntervalMillis(), sampledCounter
          .getIntervalMillis());
    }
    return counter;

  }

  @Override
  public void shutdownCounter(Counter counter) {
    if (counter instanceof SampledCounter) {
      SampledCounter sc = (SampledCounter) counter;
      sc.shutdown();
    }
  }

}
