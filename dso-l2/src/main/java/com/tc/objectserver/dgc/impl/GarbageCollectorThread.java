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
  private final boolean          doFullGC;

  public GarbageCollectorThread(final ThreadGroup group, final String name, final GarbageCollector newCollector,
                                final ObjectManagerConfig config) {
    super(group, name);
    this.collector = newCollector;
    this.doFullGC = config.doGC();
    this.fullGCSleepTime = config.gcThreadSleepTime();
    if (!this.doFullGC) {
      logger.warn("Stopping Garbage Collector thread as both DGC is disabled.");
      requestStop();
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
          // young generation GC is disabled
          doFullGC(this.fullGCSleepTime);
          lastFullGC = System.currentTimeMillis();
      }
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