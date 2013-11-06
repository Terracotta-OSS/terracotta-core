/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public class MultiResourceMonitor implements ResourceEventProducer {
  
  private static final TCLogger logger = TCLogging.getLogger(MultiResourceMonitor.class);
  private final List<ResourceEventListener> listeners = new CopyOnWriteArrayList<ResourceEventListener>();
  private final long sleepInterval;
  private final Collection<MonitoredResource> resources;
  private final EvictionThreshold  memoryThreshold;
  private final TaskRunner                        runner;
  private final Timer                             driver;
  private final int                               L2_EVICTION_CRITICALTHRESHOLD;
  private final int                               L2_EVICTION_HALTTHRESHOLD;
  
  private final Set<MonitoredResource.Type> evictions = EnumSet.noneOf(MonitoredResource.Type.class);
  private final Set<MonitoredResource.Type> throttles = EnumSet.noneOf(MonitoredResource.Type.class);
  private final Set<MonitoredResource.Type> stops = EnumSet.noneOf(MonitoredResource.Type.class);
    
  public MultiResourceMonitor(Collection<MonitoredResource> rsrc, ThreadGroup grp, EvictionThreshold et, long maxSleepTime, boolean persistent) {
    this.sleepInterval = maxSleepTime;
    this.resources = rsrc;
    this.memoryThreshold = et;
    this.runner = Runners.newScheduledTaskRunner(1, grp);
    this.driver = this.runner.newTimer();
    L2_EVICTION_CRITICALTHRESHOLD = TCPropertiesImpl.getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD,(persistent) ? 10 : -1);
    L2_EVICTION_HALTTHRESHOLD = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_EVICTION_HALTTHRESHOLD,-1);
  }

  @Override
  public void registerForResourceEvents(ResourceEventListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  @Override
  public void unregisterForResourceEvents(ResourceEventListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private synchronized void stopMonitorIfNecessary() {
    if (listeners.isEmpty()) {
      stopMonitorThread();
    }
  }

  private void stopMonitorThread() {
    driver.cancel();
    runner.shutdown();
  }

  private synchronized void startMonitorIfNecessary() {
    if (!listeners.isEmpty()) {
      for ( final MonitoredResource rsrc : this.resources ) {
        schedule(rsrc,sleepInterval);
      }
    }
  }
  
  private void schedule(final MonitoredResource rsrc, final long time) {
    switch ( rsrc.getType() ) {
      case OFFHEAP:
      case HEAP:
      case OTHER:
        driver.schedule(createMemoryTask(rsrc, time), time, TimeUnit.MILLISECONDS);
        break;
      case DISK:
        driver.schedule(createStorageTask(rsrc, time), time, TimeUnit.MILLISECONDS);
    }
    
  }
  
  private Runnable createStorageTask(final MonitoredResource rsrc, final long time) {
    return new Runnable() {

      @Override
      public void run() {
        if ( rsrc.getUsed() > rsrc.getTotal() * 0.9 ) {
          if ( stops.add(rsrc.getType()) ) {
            for (ResourceEventListener listener : listeners) {
              listener.requestStop(rsrc);
            }
          }
        } else {
          if ( stops.remove(rsrc.getType()) ) {
            for (ResourceEventListener listener : listeners) {
              listener.cancelStop(rsrc);
            }
          }
        }
        schedule(rsrc, adjust(rsrc, time));
      }
      
    };
  }
  
  private Runnable createMemoryTask(final MonitoredResource rsrc, final long time) {
    return new Runnable() {

      @Override
      public void run() {
        for (ResourceEventListener listener : listeners) {
          listener.resourcesUsed(rsrc);
        }
        if ( memoryThreshold.shouldThrottle(rsrc, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD) ) {
          for (ResourceEventListener listener : listeners) {
            listener.requestThrottle(rsrc);
          }
        } else {
            for (ResourceEventListener listener : listeners) {
              listener.cancelThrottle(rsrc);
            }
        }
        if ( memoryThreshold.isAboveThreshold(rsrc, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD) ) {
          for (ResourceEventListener listener : listeners) {
            listener.requestEvictions(rsrc);
          }
        } else {
            for (ResourceEventListener listener : listeners) {
              listener.cancelEvictions(rsrc);
            }
        }  
        schedule(rsrc, adjust(rsrc, time));
      }
      
    };
  }

  private static long adjust(MonitoredResource mu, long interval) {
    long remove = Math.round(interval * Math.sin((mu.getReserved() * Math.PI) / (2.0 * mu.getTotal())));
    return interval - (remove);
  }
    
  @Override
  public synchronized void shutdown() {
    stopMonitorThread();
  }
}
