/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ReconnectionRejectedCallback;
import com.tc.util.runtime.Os;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCMemoryManagerImpl implements TCMemoryManager, ReconnectionRejectedCallback {

  private static final TCLogger logger        = TCLogging.getLogger(TCMemoryManagerImpl.class);
  private final String          CMS_NAME      = "ConcurrentMarkSweep";
  private final String          CMS_WARN_MESG = "Terracotta does not recommend ConcurrentMarkSweep Collector.";

  private final List            listeners     = new CopyOnWriteArrayList();

  private final int             leastCount;
  private final long            sleepInterval;
  private final boolean         recommendOffheap;
  private final boolean         monitorOldGenOnly;

  private MemoryMonitor         monitor;

  private final TCThreadGroup   threadGroup;

  public TCMemoryManagerImpl(long sleepInterval, int leastCount, boolean monitorOldGenOnly, TCThreadGroup threadGroup,
                             boolean recommendOffheap) {
    this.threadGroup = threadGroup;
    verifyInput(sleepInterval, leastCount);
    this.monitorOldGenOnly = monitorOldGenOnly;
    this.leastCount = leastCount;
    this.sleepInterval = sleepInterval;
    this.recommendOffheap = recommendOffheap;
  }

  // CDV-1181 warn if using CMS
  public void checkGarbageCollectors() {
    List<GarbageCollectorMXBean> gcmbeans = ManagementFactory.getGarbageCollectorMXBeans();
    boolean foundCMS = false;
    for (GarbageCollectorMXBean mbean : gcmbeans) {
      String gcname = mbean.getName();
      logger.info("GarbageCollector: " + gcname);
      if (CMS_NAME.equals(gcname)) {
        foundCMS = true;
      }
    }
    if (foundCMS) {
      logger.warn(CMS_WARN_MESG);
    }
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
      this.monitor = new MemoryMonitor(TCRuntime.getJVMMemoryManager(), this.sleepInterval, this.monitorOldGenOnly);
      Thread t = new Thread(this.threadGroup, this.monitor);
      t.setDaemon(true);
      if (Os.isSolaris()) {
        t.setPriority(Thread.MAX_PRIORITY);
        t.setName("TC Memory Monitor(High Priority)");
      } else {
        t.setName("TC Memory Monitor");
      }
      t.start();
    }
  }

  private void fireMemoryEvent(MemoryUsage mu) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MemoryEventsListener listener = (MemoryEventsListener) i.next();
      listener.memoryUsed(mu, this.recommendOffheap);
    }
  }

  public synchronized boolean isMonitorOldGenOnly() {
    return monitor.monitorOldGenOnly();
  }

  public class MemoryMonitor implements Runnable {

    private final JVMMemoryManager manager;
    private final boolean          oldGen;
    private volatile boolean       run = true;
    private int                    lastUsed;
    private long                   sleepTime;

    public MemoryMonitor(JVMMemoryManager manager, long sleepInterval, boolean monitorOldGenOnly) {
      this.manager = manager;
      this.sleepTime = sleepInterval;
      this.oldGen = monitorOldGenOnly && manager.isMemoryPoolMonitoringSupported();
    }

    public void stop() {
      run = false;
    }

    public void run() {
      logger.debug("Starting Memory Monitor - sleep interval - " + sleepTime);
      final boolean _oldGen = oldGen;
      while (run) {
        try {
          Thread.sleep(sleepTime);
          MemoryUsage mu = (_oldGen ? manager.getOldGenUsage() : manager.getMemoryUsage());
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

    public boolean monitorOldGenOnly() {
      return oldGen;
    }
  }

  public synchronized void shutdown() {
    stopMonitorThread();
  }

}
