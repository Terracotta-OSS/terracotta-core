/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.tripwire;

import java.util.concurrent.atomic.LongAdder;
import jdk.jfr.FlightRecorder;


class StageMonitorEvent implements Monitor {

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
        if (e.hasCount()) {
          e.commit();
        }
      }
    };

  StageMonitorEvent(String target, int threads) {
    this.stage = target;
    this.threads = threads;
  }
  
  private StageEvent newEvent() {
    StageEvent e = event;
    event = new StageEvent(stage, threads);
    event.begin();
    return e;
  }
  
  @Override
  public void addItem(int backlog, long run) {
    count.increment();
    runtime.add(run);
    //  just need approx. here
    min = Math.min(min, backlog);
    max = Math.max(max, backlog);
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
    FlightRecorder.addPeriodicEvent(StageEvent.class, runnable);
  }
}
