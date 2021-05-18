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

import java.nio.file.Path;

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

  public static org.terracotta.tripwire.Event createPrimeEvent(String name, byte[] uid, long session, long id) {
    return (ENABLED) ? new PrimeEvent(name, uid, session, id) : new NullEvent();
  }

  public static org.terracotta.tripwire.Event createReplicationEvent(long session, long sequence) {
    return (ENABLED) ? new ReplicationEvent(session, sequence) : new NullEvent();
  }

  public static org.terracotta.tripwire.Event createServerStateEvent(String state, boolean active) {
    return (ENABLED) ? new ServerStateEvent(state, active) : new NullEvent();
  }

  public static org.terracotta.tripwire.Event createSyncEvent(String name, byte[] uid, long session) {
    return (ENABLED) ? new SyncEvent(name, uid, session) : new NullEvent();
  }
  
  public static org.terracotta.tripwire.StageMonitor createStageMonitor(String stage, int threads) {
    return (ENABLED) ? new StageMonitorImpl(stage, threads) : new StageMonitor() {
      @Override
      public void eventOccurred(int backlog, long value) {
      }

      @Override
      public void register() {
      }

      @Override
      public void unregister() {
      }
    };
  }
  
  public static org.terracotta.tripwire.DiskMonitor createDiskMonitor(Path path) {
    return (ENABLED) ? new DiskMonitorImpl(path) : new org.terracotta.tripwire.DiskMonitor() {
      @Override
      public void register() {
      }

      @Override
      public void unregister() {
      }
    };
  }

  public static org.terracotta.tripwire.MemoryMonitor createMemoryMonitor(String name) {
    return (ENABLED) ? new MemoryMonitorImpl(name) : new MemoryMonitor() {
      @Override
      public void sample(long free, long used) {
      }

      @Override
      public void register() {
      }

      @Override
      public void unregister() {
      }
    };
  }
  
  public static TripwireRecording createTripwireRecording(String configuration) {
    return TripwireFactory.createTripwireRecording(configuration, null, 5, 0);
  }
  
  public static TripwireRecording createTripwireRecording(String configuration, Path dest) {
    return TripwireFactory.createTripwireRecording(configuration, dest, 5, 0);
  }

  public static TripwireRecording createTripwireRecording(String configuration, Path dest, int maxAge, long maxSize) {
    if (ENABLED) {
      return new TripwireRecording(configuration, dest, maxAge, maxSize);
    } else {
      throw new UnsupportedOperationException("tripwire is unavailable");    
    }
  }
}
