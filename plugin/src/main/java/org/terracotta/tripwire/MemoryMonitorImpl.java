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
    FlightRecorder.addPeriodicEvent(StageEvent.class, runnable);
  }

  @Override
  public void unregister() {
    FlightRecorder.addPeriodicEvent(StageEvent.class, runnable);
  }
}
