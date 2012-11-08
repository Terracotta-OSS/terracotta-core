/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ReconnectionRejectedCallback;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.terracotta.corestorage.monitoring.MonitoredResource;

public class ResourceMonitor implements ReconnectionRejectedCallback {

  private static final TCLogger logger        = TCLogging.getLogger(ResourceMonitor.class);

  private final List            listeners     = new CopyOnWriteArrayList();

  private final long            sleepInterval;

  private MemoryMonitor             monitor;
  private final MonitoredResource   resource;

  private final ThreadGroup   threadGroup;

  public ResourceMonitor(MonitoredResource rsrc, long maxSleepTime, ThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.sleepInterval = maxSleepTime;
    this.resource = rsrc;
  }


  public MonitoredResource getMonitoredResource() {
      return resource;
  }
  
  public void registerForMemoryEvents(MemoryEventsListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  public void unregisterForMemoryEvents(MemoryEventsListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private synchronized void stopMonitorIfNecessary() {
    if (listeners.size() == 0) {
      stopMonitorThread();
    }
  }

  /**
   * XXX: Should we wait for the monitor thread to stop completely.
   */
  private void stopMonitorThread() {
    if (monitor != null) {
      monitor.stop();
      monitor = null;
    }
  }

  private synchronized void startMonitorIfNecessary() {
    if (listeners.size() > 0 && monitor == null) {
      this.monitor = new MemoryMonitor(this.sleepInterval);
      Thread t = new Thread(this.threadGroup, this.monitor);
      t.setDaemon(true);
      t.setName("Resource Monitor");
      t.start();
    }
  }

  private void fireMemoryEvent(MemoryUsage mu) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MemoryEventsListener listener = (MemoryEventsListener) i.next();
      listener.memoryUsed(mu, false);
    }
  }

  public class MemoryMonitor implements Runnable {

    private volatile boolean       run = true;
    private long                   sleepTime;

    public MemoryMonitor(long sleepInterval) {
      this.sleepTime = sleepInterval;
    }

    public void stop() {
      run = false;
    }

    public void run() {
      logger.debug("Starting Memory Monitor - sleep interval - " + sleepTime);
      long counter = 0;
      while (run) {
        try {
          final long thisCount = counter++;
          
          MemoryUsage mu = new MemoryUsage() {
              long cacheMax = -1;
              long cacheUsed = -1;
              
              private long checkUsed() {
                  if ( cacheUsed < 0 ) {
                      cacheUsed = resource.getReserved();
                  }
                  return cacheUsed;
              }
              
              private long checkMax() {
                  if ( cacheMax < 0 ) {
                      cacheMax = resource.getTotal();
                  }
                  return cacheMax;
              }              
              
                @Override
                public long getFreeMemory() {
                    return checkMax() - checkUsed();
                }

                @Override
                public String getDescription() {
                    return resource.getType().toString();
                }

                @Override
                public long getMaxMemory() {
                    return checkMax();
                }

                @Override
                public long getUsedMemory() {
                    return checkUsed();
                }

                @Override
                public int getUsedPercentage() {
                    float num = checkUsed();
                    float denom = checkMax();
                    return Math.round((num/denom)*100f);
                }

                @Override
                public long getCollectionCount() {
                    return thisCount;
                }

                @Override
                public long getCollectionTime() {
                    return 0;
                }
            };
          fireMemoryEvent(mu);
          adjust(mu);
          Thread.sleep(sleepTime);
        } catch (Throwable t) {
          // for debugging pupose
          StackTraceElement[] trace = t.getStackTrace();
          for (StackTraceElement element : trace)
            logger.warn(element.toString());
          logger.error(t);
          throw new TCRuntimeException(t);
        }
      }
      logger.debug("Stopping Memory Monitor - sleep interval - " + sleepTime);
    }

    private void adjust(MemoryUsage mu) {
      int remove = Math.round(sleepInterval * (mu.getUsedMemory()*1f / mu.getMaxMemory()));
      sleepTime = sleepInterval - remove;
    }
  }

  public synchronized void shutdown() {
    stopMonitorThread();
  }

}
