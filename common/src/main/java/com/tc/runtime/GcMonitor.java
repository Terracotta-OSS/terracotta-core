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
 */
package com.tc.runtime;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ListenerNotFoundException;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GcMonitor {

  private static final Logger logger = LoggerFactory.getLogger(GcMonitor.class);

  private final Queue<GcStats> gcStatsQueue = new ConcurrentLinkedQueue<>();
  private final Timer timer = new Timer("GcMonitor-timer", true);
  private final NotificationListener listener = (notification, handback) -> {
    if (notification.getType().equals("com.sun.management.gc.notification")) {
      CompositeData userData = (CompositeData) notification.getUserData();
      CompositeData gcInfo = (CompositeData) userData.get("gcInfo");

      GcStats gcStats = new GcStats(
          (Long) gcInfo.get("duration"),
          (String) userData.get("gcAction"),
          (Long) gcInfo.get("startTime"),
          (String) userData.get("gcCause"),
          (String) userData.get("gcName")
      );
      gcStatsQueue.add(gcStats);
    }
  };


  public GcMonitor() {
  }

  public void init() {
    long delay = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.TC_GC_MONITOR_DELAY);

    List<GarbageCollectorMXBean> gcMxBeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
    for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
      NotificationEmitter emitter = (NotificationEmitter)gcMxBean;
      emitter.addNotificationListener(listener, null, null);
    }
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        dump();
      }
    }, delay, delay);
  }

  public void close() {
    List<GarbageCollectorMXBean> gcMxBeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
    for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
      NotificationEmitter emitter = (NotificationEmitter)gcMxBean;

      try {
        emitter.removeNotificationListener(listener);
      } catch (ListenerNotFoundException e) {
        //
      }
    }
    timer.cancel();
  }

  private void dump() {
    while (true) {
      GcStats gcStats = gcStatsQueue.poll();
      if (gcStats == null) break;
      logger.info("GC event : startTime={} / duration={} / action={} / cause={} / name={}", gcStats.startTimestamp, gcStats.duration, gcStats.action, gcStats.cause, gcStats.name);
    }

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    java.lang.management.MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
    logger.info("Heap usage : init={} / used={} / committed={} / max={}", heapUsage.getInit(), heapUsage.getUsed(), heapUsage.getCommitted(), heapUsage.getMax());
  }


  private static class GcStats {
    private final long startTimestamp;
    private final long duration;
    private final String action;
    private final String cause;
    private final String name;

    private GcStats(long duration, String action, long startTimestamp, String cause, String name) {
      this.duration = duration;
      this.action = action;
      this.startTimestamp = startTimestamp;
      this.cause = cause;
      this.name = name;
    }
  }

}
