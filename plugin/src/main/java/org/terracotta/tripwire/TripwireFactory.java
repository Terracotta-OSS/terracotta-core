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

/**
 *
 */
public class TripwireFactory {
  private static final boolean ENABLED;
  
  static {
    boolean hasJFR = false;
    try {
      Class<?> jfr = Class.forName("jdk.jfr.Event");
      hasJFR = (jfr != null);
    } catch (ClassNotFoundException c) {
      
    }
    ENABLED = hasJFR;
  }
  
  public static org.terracotta.tripwire.Event createMessageEvent(String eid, int concurrency, String action, long source, String instance, long transaction, String trace) {
    return (ENABLED) ? new MessageEvent(eid, concurrency, action, source, instance, transaction, trace) : new NullEvent();
  }
  
  public static org.terracotta.tripwire.Event createStageEvent(String stage, String debug) {
    return (ENABLED) ? new MonitoringEvent(stage, debug) : new NullEvent();
  }
  
  public static org.terracotta.tripwire.Monitor createStageMonitor(String stage, int threads) {
    return (ENABLED) ? new StageMonitorEvent(stage, threads) : new NullMonitor();
  }
  
  public static TripwireRecording createTripwireRecording(String configuration) {
    if (!ENABLED) {
      throw new UnsupportedOperationException("tripwire is unavailable");    
    }
    return new TripwireRecording(configuration);
  }
}
