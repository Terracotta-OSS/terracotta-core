/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.impl;

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
  
  private final List<ResourceEventListener> listeners = new CopyOnWriteArrayList<ResourceEventListener>();
  private final long sleepInterval;
  private final Collection<MonitoredResource> resources;
  private final EvictionThreshold  memoryThreshold;
  private final TaskRunner                        runner;
  private final Timer                             driver;
  private final int                               L2_EVICTION_CRITICALTHRESHOLD;
  private final int                               L2_EVICTION_HALTTHRESHOLD;
  private final int                               L2_EVICTION_OFFHEAP_STOPPAGE;
  private final int                               L2_EVICTION_STORAGE_STOPPAGE;
  private final boolean                           flash;
  
  private boolean evicting = false;
  private boolean throttling = false;
  private final Set<MonitoredResource.Type> stops = EnumSet.noneOf(MonitoredResource.Type.class);
    
  public MultiResourceMonitor(Collection<MonitoredResource> rsrc, ThreadGroup grp, EvictionThreshold et, long maxSleepTime, boolean flash, boolean persistent) {
    this.sleepInterval = maxSleepTime;
    this.resources = rsrc;
    this.memoryThreshold = et;
    this.runner = Runners.newScheduledTaskRunner(1, grp);
    this.driver = this.runner.newTimer();
    this.flash = flash;
    L2_EVICTION_CRITICALTHRESHOLD = TCPropertiesImpl.getProperties()
            .getInt(TCPropertiesConsts.L2_EVICTION_CRITICALTHRESHOLD,(persistent) ? 10 : -1);
    L2_EVICTION_HALTTHRESHOLD = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_EVICTION_HALTTHRESHOLD,-1);
    L2_EVICTION_OFFHEAP_STOPPAGE = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_EVICTION_OFFHEAP_STOPPAGE,8 * 1024 * 1024);
    L2_EVICTION_STORAGE_STOPPAGE = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_EVICTION_STORAGE_STOPPAGE,95);
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
    for ( MonitoredResource rsrc : this.resources ) {
      switch ( rsrc.getType() ) {
        case OFFHEAP:
          if ( flash ) {
            schedule(createVitalMemoryTask(rsrc, sleepInterval), 10);
          } else {
            schedule(createMemoryTask(rsrc, sleepInterval), 20);
          }
        break;
        case HEAP:
        case OTHER:
          schedule(createMemoryTask(rsrc, sleepInterval), 30);
          break;
        case DISK:
          schedule(createStorageTask(rsrc, sleepInterval), 40);
      }
    }
  }
  
  private void schedule(final Runnable runnable, final long time) {
    driver.schedule(runnable, time, TimeUnit.MILLISECONDS);    
  }
  
  private Runnable createStorageTask(final MonitoredResource rsrc, final long time) {
    return new Runnable() {

      @Override
      public void run() {
        long reserved = rsrc.getReserved();
        long total = rsrc.getTotal();
        if ( reserved > total * (L2_EVICTION_STORAGE_STOPPAGE*0.01) ) {
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
        schedule(this, adjust(reserved,total, time));
      }
    };
  }
  
  private Runnable createVitalMemoryTask(final MonitoredResource rsrc, final long time) {
    return new Runnable() {

      @Override
      public void run() {
        long vital = rsrc.getVital();
        long total = rsrc.getTotal();

        if ( total - vital < L2_EVICTION_OFFHEAP_STOPPAGE ) {
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
        schedule(this, adjust(vital ,total , time));        
      }
    };
  }
  
  private Runnable createMemoryTask(final MonitoredResource rsrc, final long time) {
    return new Runnable() {

      @Override
      public void run() {
        MonitoredResource working = new CachingMonitoredResource(rsrc);
        for (ResourceEventListener listener : listeners) {
          listener.resourcesUsed(working);
        }
        
        if ( memoryThreshold.shouldThrottle(working, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD) ) {
          throttling = true;
          for (ResourceEventListener listener : listeners) {
            listener.requestThrottle(working);
          }
        } else {
            if ( throttling ) {
              for (ResourceEventListener listener : listeners) {
                listener.cancelThrottle(working);
              }
            }
        }
        if ( memoryThreshold.isAboveThreshold(working, L2_EVICTION_CRITICALTHRESHOLD, L2_EVICTION_HALTTHRESHOLD) ) {
          evicting = true;
          for (ResourceEventListener listener : listeners) {
            listener.requestEvictions(working);
          }
        } else {
          if ( evicting ) {
            for (ResourceEventListener listener : listeners) {
              listener.cancelEvictions(working);
            }
          }
        }
        schedule(this, adjust(working.getReserved(),working.getTotal(),time));
      }
    };
  }

  private static long adjust(long reserved, long total, long interval) {
    long remove = Math.round(interval * Math.sin((reserved * Math.PI) / (2.0 * total)));
    return interval - (remove);
  }
    
  @Override
  public synchronized void shutdown() {
    stopMonitorThread();
  }
  
  private static class CachingMonitoredResource implements MonitoredResource {
    private final MonitoredResource delegate;
    private long vital;
    private long used;
    private long reserved;
    private long total;

    public CachingMonitoredResource(MonitoredResource delegate) {
      this.delegate = delegate;
    }

    @Override
    public Type getType() {
      return delegate.getType();
    }

    @Override
    public long getVital() {
      if ( vital == 0 ) {
        vital = delegate.getVital();
      }
      return vital;
    }

    @Override
    public long getUsed() {
      if ( used == 0 ) {
        used = delegate.getUsed();
      }
      return used;
    }

    @Override
    public long getReserved() {
      if ( reserved == 0 ) {
        reserved = delegate.getReserved();
      }
      return reserved;
    }

    @Override
    public long getTotal() {
      if ( total == 0 ) {
        total = delegate.getTotal();
      }
      return total;
    }

    @Override
    public Runnable addUsedThreshold(Direction direction, long value, Runnable action) {
      return delegate.addUsedThreshold(direction, value, action);
    }

    @Override
    public Runnable removeUsedThreshold(Direction direction, long value) {
      return delegate.removeUsedThreshold(direction, value);
    }

    @Override
    public Runnable addReservedThreshold(Direction direction, long value, Runnable action) {
      return delegate.addReservedThreshold(direction, value, action);
    }

    @Override
    public Runnable removeReservedThreshold(Direction direction, long value) {
      return delegate.removeReservedThreshold(direction, value);
    }
    
    
  }
}
