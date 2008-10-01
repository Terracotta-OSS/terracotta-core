/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TCMemoryManagerImpl implements TCMemoryManager {

  private static final TCLogger logger    = TCLogging.getLogger(TCMemoryManagerImpl.class);

  private final List            listeners = new ArrayList();

  private int                   leastCount;
  private final long            sleepInterval;
  private final boolean         monitorOldGenOnly;

  private MemoryMonitor         monitor;

  private final TCThreadGroup   threadGroup;

  public TCMemoryManagerImpl(long sleepInterval, int leastCount, boolean monitorOldGenOnly, TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    verifyInput(sleepInterval, leastCount);
    this.monitorOldGenOnly = monitorOldGenOnly;
    this.leastCount = leastCount;
    this.sleepInterval = sleepInterval;
  }

  private void verifyInput(long sleep, int lc) {
    if (sleep <= 0) { throw new AssertionError("Sleep Interval cannot be <= 0 : sleep Interval = " + sleep); }
    if (lc <= 0 || lc >= 100) { throw new AssertionError("Least Count should be > 0 && < 100 : " + lc
                                                         + " Outside range"); }
  }

  public synchronized void registerForMemoryEvents(MemoryEventsListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  public synchronized void unregisterForMemoryEvents(MemoryEventsListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private void stopMonitorIfNecessary() {
    if (listeners.size() == 0 && monitor != null) {
      monitor.stop();
      monitor = null;
    }
  }

  private void startMonitorIfNecessary() {
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

  private synchronized void fireMemoryEvent(MemoryUsage mu) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MemoryEventsListener listener = (MemoryEventsListener) i.next();
      listener.memoryUsed(mu);
    }
  }

  public boolean isMonitorOldGenOnly() {
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
}
