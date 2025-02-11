/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.tripwire;

import java.util.concurrent.atomic.LongAdder;
import jdk.jfr.FlightRecorder;


class StageMonitorImpl implements StageMonitor {

  private final String stage;
  private final int threads;
  private final LongAdder count = new LongAdder();
  private final LongAdder runtime = new LongAdder();
  private volatile StageEvent event;
  private volatile int min = Integer.MAX_VALUE;
  private volatile int max = 0;

  private final Runnable runnable = ()-> {
      StageEvent e = newEvent();
      if (e != null) {
        e.setStats(getCount(), min, max, getRuntime());
        min = Integer.MAX_VALUE;
        max = 0;
        if (e.hasCount()) {
          e.commit();
        }
      }
    };

  StageMonitorImpl(String target, int threads) {
    this.stage = target;
    this.threads = threads;
  }
  
  private StageEvent newEvent() {
    StageEvent e = event;
    event = new StageEvent(stage, threads);
    event.begin();
    return e;
  }
  
  public void eventOccurred(int backlog, long run) {
    //  just need approx. here
    min = (int)Math.min(min, backlog);
    max = (int)Math.max(max, backlog); 
    runtime.add(run);
    count.increment();
  }
  
  private int getCount() {
    return (int)count.sumThenReset();
  }
  
  private long getRuntime() {
    return runtime.sumThenReset();
  }
  
  @Override
  public void register() {
    FlightRecorder.addPeriodicEvent(StageEvent.class, runnable);
  }

  @Override
  public void unregister() {
    FlightRecorder.removePeriodicEvent(runnable);
  }
}
