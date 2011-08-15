/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.util.concurrent.StoppableThread;

public class GarbageCollectorThread extends StoppableThread {
  private static final TCLogger  logger   = TCLogging.getLogger(GarbageCollectorThread.class);

  private final GarbageCollector collector;
  private final Object           stopLock = new Object();
  private final long             fullGCSleepTime;
  private final long             youngGCSleepTime;
  private final boolean          doFullGC;

  public GarbageCollectorThread(final ThreadGroup group, final String name, final GarbageCollector newCollector,
                                final ObjectManagerConfig config) {
    super(group, name);
    this.collector = newCollector;
    this.doFullGC = config.doGC();
    this.fullGCSleepTime = config.gcThreadSleepTime();
    long youngGCTime = -1;
    if (config.isYoungGenDGCEnabled()) {
      youngGCTime = config.getYoungGenDGCFrequencyInMillis();
      if (this.doFullGC && youngGCTime >= this.fullGCSleepTime) {
        logger.warn("Disabling YoungGen Garbage Collector since the time interval for YoungGen GC ( " + youngGCTime
                    + " ms) is greater than or equal to  the time interval for Full GC ( " + this.fullGCSleepTime
                    + " ms)");
        youngGCTime = -1;
      } else if (youngGCTime <= 0) {
        logger.warn("Disabling YoungGen GC since time interval is specificed as " + youngGCTime + " ms");
        youngGCTime = -1;
      }
    }
    if ((this.youngGCSleepTime = youngGCTime) == -1 && !this.doFullGC) {
      logger.warn("Stopping Garbage Collector thread as both Full and YoungGen collectors are disabled.");
      requestStop();
    } else {
      logger.info("Young Gen Time = " + youngGCTime + " Full Gen Time = " + this.fullGCSleepTime);
    }
  }

  @Override
  public void requestStop() {
    super.requestStop();

    synchronized (this.stopLock) {
      this.stopLock.notifyAll();
    }
  }

  @Override
  public void run() {
    long lastFullGC = System.currentTimeMillis();
    while (true) {
      if (isStopRequested()) { return; }
      if (this.doFullGC) {
        if (this.youngGCSleepTime <= 0) {
          // young generation GC is disabled
          doFullGC(this.fullGCSleepTime);
          lastFullGC = System.currentTimeMillis();
        } else {
          // run young or full GC
          final long current = System.currentTimeMillis();
          if (lastFullGC + this.fullGCSleepTime > current + this.youngGCSleepTime) {
            // Run young GC next
            doYoungGC(this.youngGCSleepTime);
          } else {
            // Run full GC next, sleeping at least 1 second
            doFullGC(Math.max(lastFullGC + this.fullGCSleepTime - current, 1000));
            lastFullGC = System.currentTimeMillis();
          }
        }
      } else {
        doYoungGC(this.youngGCSleepTime);
      }
    }
  }

  private void doYoungGC(final long sleepTime) {
    try {
      synchronized (this.stopLock) {
        this.stopLock.wait(sleepTime);
      }
      if (isStopRequested()) { return; }
      this.collector.doGC(GCType.YOUNG_GEN_GC);
    } catch (final InterruptedException ie) {
      throw new TCRuntimeException(ie);
    }
  }

  private void doFullGC(final long sleepTime) {
    try {
      synchronized (this.stopLock) {
        this.stopLock.wait(sleepTime);
      }
      if (isStopRequested()) { return; }
      this.collector.doGC(GCType.FULL_GC);
    } catch (final InterruptedException ie) {
      throw new TCRuntimeException(ie);
    }
  }
}