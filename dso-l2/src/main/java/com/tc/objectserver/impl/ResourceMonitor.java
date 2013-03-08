/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ReconnectionRejectedCallback;
import com.tc.objectserver.api.ShutdownError;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResourceMonitor implements ReconnectionRejectedCallback {

  private static final TCLogger logger = TCLogging.getLogger(ResourceMonitor.class);

  private final List<ResourceEventListener> listeners = new CopyOnWriteArrayList<ResourceEventListener>();

  private final long sleepInterval;

  private MemoryMonitor monitor;
  private final MonitoredResource resource;

  private final ThreadGroup threadGroup;

  public ResourceMonitor(MonitoredResource rsrc, long maxSleepTime, ThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.sleepInterval = maxSleepTime;
    this.resource = rsrc;
  }

  public MonitoredResource getMonitoredResource() {
    return resource;
  }

  public void registerForResourceEvents(ResourceEventListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  public void unregisterForResourceEvents(ResourceEventListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private synchronized void stopMonitorIfNecessary() {
    if (listeners.isEmpty()) {
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

  private void fireMemoryEvent(final MonitoredResource resourceParam, final long count) {
    DetailedMemoryUsage usage = new DetailedMemoryUsage(resourceParam, count);
    for (ResourceEventListener listener : listeners) {
      listener.resourcesUsed(usage);
    }
  }

  public class MemoryMonitor implements Runnable {

    private volatile boolean run = true;
    private long sleepTime;

    public MemoryMonitor(long sleepInterval) {
      this.sleepTime = sleepInterval;
    }

    public void stop() {
      run = false;
    }

    @Override
    public void run() {
      logger.debug("Starting Memory Monitor - sleep interval - " + sleepTime);
      long counter = 0;
      while (run) {
        try {
          final long thisCount = counter++;

          fireMemoryEvent(resource, thisCount);
          adjust(resource);
          Thread.sleep(sleepTime);
        } catch (ShutdownError e) {
          logger.warn("Server is shutting down, stopping resource monitor.");
          break;
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

    private void adjust(MonitoredResource mu) {
      long remove = Math.round(sleepInterval * Math.sin((mu.getReserved() * Math.PI) / (2.0 * mu.getTotal())));
      sleepTime = sleepInterval - (remove);
    }
  }

  public long getOffset() {
    long used = resource.getUsed();
    long reserved = resource.getReserved();
    logger.info("MEMCHECK used:" + used + " - " + reserved + ":reserved");
    if (used < reserved * .75 || used > reserved * 1.25) {
      return reserved - used;
    } else {
      return 0;
    }
  }

  @Override
  public synchronized void shutdown() {
    stopMonitorThread();
  }

  static class MemoryConsumer implements MemoryEventsListener {
    private final MemoryEventsListener delegate;
    private final boolean detailed;

    MemoryConsumer(MemoryEventsListener delegate, boolean detailed) {
      this.delegate = delegate;
      this.detailed = detailed;
    }

    @Override
    public void memoryUsed(MemoryUsage usage) {
      delegate.memoryUsed(usage);
    }

    public boolean isDetailed() {
      return detailed;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof MemoryConsumer) { return ((MemoryConsumer)o).delegate == delegate; }
      return false;
    }

  }

}
