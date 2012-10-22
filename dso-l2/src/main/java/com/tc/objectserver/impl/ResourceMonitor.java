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

  private final int             leastCount;
  private final long            sleepInterval;

  private MemoryMonitor             monitor;
  private final MonitoredResource   resource;

  private final TCThreadGroup   threadGroup;

  public ResourceMonitor(MonitoredResource rsrc, long sleepInterval, int leastCount,  TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    verifyInput(sleepInterval, leastCount);
    this.leastCount = leastCount;
    this.sleepInterval = sleepInterval;
    this.resource = rsrc;
  }

  private void verifyInput(long sleep, int lc) {
    if (sleep <= 0) { throw new AssertionError("Sleep Interval cannot be <= 0 : sleep Interval = " + sleep); }
    if (lc <= 0 || lc >= 100) { throw new AssertionError("Least Count should be > 0 && < 100 : " + lc
                                                         + " Outside range"); }
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
    private int                    lastUsed;
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
          Thread.sleep(sleepTime);
          final long thisCount = counter++;
          
          MemoryUsage mu = new MemoryUsage() {
                @Override
                public long getFreeMemory() {
                    return resource.getTotal() - resource.getReserved();
                }

                @Override
                public String getDescription() {
                    return resource.getType().toString();
                }

                @Override
                public long getMaxMemory() {
                    return resource.getTotal();
                }

                @Override
                public long getUsedMemory() {
                    return resource.getReserved();
                }

                @Override
                public int getUsedPercentage() {
                    float num = resource.getUsed();
                    float denom = resource.getTotal();
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
      int usedPercentage = mu.getUsedPercentage();
      try {
        if (lastUsed != 0 && lastUsed < usedPercentage) {
          int diff = usedPercentage - lastUsed;
          long l_sleep = this.sleepTime;
          if (diff > leastCount * 1.5 && l_sleep > 1) {
            // decrease sleep time
            this.sleepTime = Math.max(1, l_sleep * leastCount / diff);
            logger.info("Sleep time changed to : " + this.sleepTime);
          } else if (diff < leastCount * 0.5 && l_sleep < sleepInterval) {
            // increase sleep time
            this.sleepTime = Math.min(sleepInterval, l_sleep * leastCount / diff);
            logger.info("Sleep time changed to : " + this.sleepTime);
          }
        }
      } finally {
        lastUsed = usedPercentage;
      }
    }
  }

  public synchronized void shutdown() {
    stopMonitorThread();
  }

}
