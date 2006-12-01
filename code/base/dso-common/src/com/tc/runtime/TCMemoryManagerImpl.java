/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.DebugUtil;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TCMemoryManagerImpl implements TCMemoryManager {

  private static final TCLogger logger    = TCLogging.getLogger(TCMemoryManagerImpl.class);

  private final List            listeners = new ArrayList();

  private final int             threshold;
  private final int             criticalThreshold;
  private final int             leastCount;
  private final long            sleepInterval;
  private final boolean         monitorOldGenOnly;

  private MemoryMonitor         monitor;

  public TCMemoryManagerImpl(int usedThreshold, int usedCriticalThreshold, long sleepInterval, int leastCount,
                             boolean monitorOldGenOnly) {
    verifyInput(usedThreshold, usedCriticalThreshold, sleepInterval, leastCount);
    this.monitorOldGenOnly = monitorOldGenOnly;
    this.leastCount = leastCount;
    this.threshold = usedThreshold;
    this.criticalThreshold = usedCriticalThreshold;
    this.sleepInterval = sleepInterval;
  }

  private void verifyInput(int usedT, int usedCT, long sleep, int lc) {
    if (usedT <= 0 || usedT >= 100) {
      //
      throw new AssertionError("Used Threshold should be > 0 && < 100 : " + usedT + " Outside range");
    }
    if (usedCT <= 0 || usedCT >= 100) {
      //
      throw new AssertionError("Critical Used Threshold should be > 0 && < 100 : " + usedCT + " Outside range");
    }
    if (usedT > usedCT) {
      //
      throw new AssertionError("Used Threshold should be <= Critical Used Threshold : " + usedT + " <= " + usedCT);
    }
    if (sleep <= 0) {
      //
      throw new AssertionError("Sleep Interval cannot be <= 0 : sleep Interval = " + sleep);
    }
    if (lc <= 0 || lc >= 100) {
      //
      throw new AssertionError("Least Count should be > 0 && < 100 : " + lc + " Outside range");
    }
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
      Thread t = new Thread(this.monitor);
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

  private synchronized void fireMemoryEvent(MemoryEventType type, MemoryUsage mu) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MemoryEventsListener listener = (MemoryEventsListener) i.next();
      listener.memoryUsed(type, mu);
    }
  }

  public class MemoryMonitor implements Runnable {

    private final JVMMemoryManager manager;
    private final boolean          oldGen;
    private volatile boolean       run = true;
    private int                    lastUsed;
    private MemoryUsage            lastReported;
    private long                   sleepTime;
    private MemoryEventType        currentState;

    public MemoryMonitor(JVMMemoryManager manager, long sleepInterval, boolean monitorOldGenOnly) {
      this.manager = manager;
      this.sleepTime = sleepInterval;
      this.oldGen = monitorOldGenOnly && manager.isMemoryPoolMonitoringSupported();
      this.currentState = MemoryEventType.BELOW_THRESHOLD;
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
          if (DebugUtil.DEBUG) {
            logger.info("Memory Usage is : " + mu);
          }
          reportUsage(mu);
          adjust(mu);
        } catch (Throwable t) {
          logger.error(t);
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

    private void reportUsage(MemoryUsage mu) {
      int usedPercentage = mu.getUsedPercentage();
      if (usedPercentage < threshold) {
        if (currentState != MemoryEventType.BELOW_THRESHOLD) {
          // Send only 1 BELOW_THRESHOLD event
          fire(MemoryEventType.BELOW_THRESHOLD, mu);
        }
      } else if (usedPercentage >= criticalThreshold) {
        if (!oldGen || currentState != MemoryEventType.ABOVE_CRITICAL_THRESHOLD || isLeastCountReached(usedPercentage)
            || isGCCompleted(mu)) {
          // Send an event every time if we are monitoring the entire heap or else if we are monitoring only old gen
          // then send an event only if greater than least count or if we just reached ABOVE_CRITICAL_THRESHOLD or if a gc took place
          fire(MemoryEventType.ABOVE_CRITICAL_THRESHOLD, mu);
        }
      } else if (currentState != MemoryEventType.ABOVE_THRESHOLD || isLeastCountReached(usedPercentage)) {
        fire(MemoryEventType.ABOVE_THRESHOLD, mu);
      }
    }

    private boolean isGCCompleted(MemoryUsage mu) {
      return lastReported.getCollectionCount() < mu.getCollectionCount();
    }

    private boolean isLeastCountReached(int usedPercentage) {
      return (Math.abs(usedPercentage - lastReported.getUsedPercentage()) >= leastCount);
    }

    private void fire(MemoryEventType type, MemoryUsage mu) {
      fireMemoryEvent(type, mu);
      currentState = type;
      lastReported = mu;
    }
  }

}
