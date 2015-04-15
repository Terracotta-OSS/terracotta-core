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

  @Override
  public synchronized void shutdown() {
    if (shutdown) { return; }
    try {
      timer.cancel();
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
