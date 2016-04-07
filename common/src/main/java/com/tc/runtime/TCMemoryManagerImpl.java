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
package com.tc.runtime;

import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.runtime.Os;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCMemoryManagerImpl implements TCMemoryManager {

  private static final TCLogger            logger        = TCLogging.getLogger(TCMemoryManagerImpl.class);
  private static final String              CMS_NAME      = "ConcurrentMarkSweep";
  private static final String              CMS_WARN_MESG = "Terracotta does not recommend ConcurrentMarkSweep Collector.";

  private final List<MemoryEventsListener> listeners     = new CopyOnWriteArrayList<MemoryEventsListener>();

  private final int                        leastCount;
  private final long                       sleepInterval;

  private MemoryMonitor                    monitor;

  private final TCThreadGroup              threadGroup;

  public TCMemoryManagerImpl(long sleepInterval, int leastCount, TCThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
    this.sleepInterval = sleepInterval;
    this.leastCount = leastCount;
  }

  public TCMemoryManagerImpl(TCThreadGroup threadGroup) {
    this(3000, 2, threadGroup);
  }

  // CDV-1181 warn if using CMS
  @Override
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

  @Override
  public void registerForMemoryEvents(MemoryEventsListener listener) {
    listeners.add(listener);
    startMonitorIfNecessary();
  }

  @Override
  public void unregisterForMemoryEvents(MemoryEventsListener listener) {
    listeners.remove(listener);
    stopMonitorIfNecessary();
  }

  private synchronized void stopMonitorIfNecessary() {
    if (listeners.size() == 0) {
      stopMonitorThread();
    }
  }

  private void stopMonitorThread() {
    if (monitor != null) {
      monitor.stopMonitoring();
      try {
        monitor.join();
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }
  }

  private synchronized void startMonitorIfNecessary() {
    if (listeners.size() > 0 && monitor == null) {
      this.monitor = new MemoryMonitor(TCRuntime.getJVMMemoryManager(), sleepInterval);
      monitor.start();
    }
  }

  private void fireMemoryEvent(MemoryUsage mu) {
    for (MemoryEventsListener listener : listeners) {
      listener.memoryUsed(mu);
    }
  }

  public class MemoryMonitor extends Thread {

    private final JVMMemoryManager manager;
    private volatile boolean       run = true;
    private int                    lastUsed;
    private long                   sleepTime;

    public MemoryMonitor(JVMMemoryManager manager, long sleepInterval) {
      super(threadGroup, "TC Memory Monitor");
      if (Os.isSolaris()) {
        monitor.setPriority(Thread.MAX_PRIORITY);
        monitor.setName("TC Memory Monitor(High Priority)");
      }
      this.manager = manager;
      this.sleepTime = sleepInterval;
    }

    private void stopMonitoring() {
      run = false;
      this.interrupt();
    }

    @Override
    public void run() {
      logger.debug("Starting Memory Monitor - sleep interval - " + sleepTime);
      boolean interrupt = false;
      while (run) {
        try {
          Thread.sleep(sleepTime);
          MemoryUsage mu = manager.isMemoryPoolMonitoringSupported() ? manager.getOldGenUsage() : manager.getMemoryUsage();
          fireMemoryEvent(mu);
          adjust(mu);
        } catch (InterruptedException ie) {
          if (run) {
            interrupt = true;
          }
        } catch (Throwable t) {
          // for debugging purpose
          StackTraceElement[] trace = t.getStackTrace();
          for (StackTraceElement element : trace)
            logger.warn(element.toString());
          logger.error(t);
          throw new TCRuntimeException(t);
        }
      }
      if (interrupt) {
        this.interrupt();
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

  @Override
  public synchronized void shutdown() {
    stopMonitorThread();
  }

}
