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

import java.util.function.Supplier;
import jdk.jfr.FlightRecorder;


class ClusterInfoMonitorImpl implements ClusterInfoMonitor {

  private final Runnable runnable;

  ClusterInfoMonitorImpl(Supplier<String> supplier) {
    this.runnable = ()-> {
      newEvent(supplier.get()).commit();
    };
  }
  
  private ClusterInfoEvent newEvent(String info) {
    return new ClusterInfoEvent(info);
  }
  
  @Override
  public void register() {
    FlightRecorder.addPeriodicEvent(ClusterInfoEvent.class, runnable);
  }

  @Override
  public void unregister() {
    FlightRecorder.removePeriodicEvent(runnable);
  }
}
