/*
 * Copyright IBM Corp. 2024, 2025
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

import jdk.jfr.FlightRecorder;


class MemoryMonitorImpl implements MemoryMonitor {

  private final String description;
  private volatile long free = Long.MAX_VALUE;
  private volatile long used = Long.MIN_VALUE;

  private final Runnable runnable = ()-> {
    newEvent().commit();
  };

  MemoryMonitorImpl(String name) {
    this.description = name;
  }
  
  private MemoryEvent newEvent() {
    return new MemoryEvent(description, free, used);
  }
  
  @Override
  public void sample(long free, long used) {
    this.free = Math.min(this.free, free);
    this.used = Math.max(this.used, used);
  }
  
  @Override
  public void register() {
    FlightRecorder.addPeriodicEvent(MemoryEvent.class, runnable);
  }

  @Override
  public void unregister() {
    FlightRecorder.removePeriodicEvent(runnable);
  }
}
