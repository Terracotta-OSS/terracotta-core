/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.text.PrettyPrinter;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.NullLifeCycleState;

import java.util.concurrent.TimeUnit;

public abstract class AbstractGarbageCollector implements GarbageCollector {
  private static final TCLogger         logger               = TCLogging.getLogger(GarbageCollector.class);
  private static final int              WAIT_INTERVAL        = 10 * 1000;
  private static final int              WAIT_LOG_THRESHOLD   = 10 * 1000;

  protected static final LifeCycleState NULL_LIFECYCLE_STATE = new NullLifeCycleState();

  private volatile State                state                = GC_SLEEP;
  private volatile boolean              periodicEnabled      = false;

  public synchronized boolean requestGCStart() {
    if (isStarted() && this.state == GC_SLEEP) {
      this.state = GC_RUNNING;
      return true;
    }
    // Can't start DGC
    return false;
  }

  public synchronized void enableGC() {
    if (GC_DISABLED == this.state) {
      this.state = GC_SLEEP;
      notify();
    } else {
      logger.warn("DGC is already enabled : " + this.state);
    }
  }

  public synchronized boolean requestDisableGC() {
    if (GC_SLEEP == this.state) {
      this.state = GC_DISABLED;
      return true;
    }
    // DGC is already running, can't be disabled
    logger.warn("DGC can't be disabled since its current state is: " + this.state);
    return false;
  }

  public synchronized void notifyReadyToGC() {
    if (this.state == GC_PAUSING) {
      this.state = GC_PAUSED;
    }
  }

  public synchronized void notifyGCComplete() {
    this.state = GC_SLEEP;
    notify();
  }

  /**
   * In Active server, state transitions from GC_PAUSED to GC_DELETE and in the passive server, state transitions from
   * GC_SLEEP to GC_DELETE.
   */
  protected synchronized boolean requestGCDeleteStart() {
    if (this.state == GC_SLEEP || this.state == GC_PAUSED) {
      this.state = GC_DELETE;
      return true;
    }
    return false;
  }

  public void requestGCPause() {
    this.state = GC_PAUSING;
  }

  public boolean isPausingOrPaused() {
    State localState = this.state;
    return GC_PAUSED == localState || GC_PAUSING == localState;
  }

  public boolean isPaused() {
    return this.state == GC_PAUSED;
  }

  public boolean isDisabled() {
    return GC_DISABLED == this.state;
  }

  public void setPeriodicEnabled(final boolean periodicEnabled) {
    this.periodicEnabled = periodicEnabled;
  }

  public boolean isPeriodicEnabled() {
    return periodicEnabled;
  }

  public synchronized void waitToStartGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestGCStart()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms to start DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized void waitToDisableGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestDisableGC()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms to disable DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized void waitToStartInlineGC() {
    boolean isInterrupted = false;
    long lastLogTime = System.nanoTime();
    final long startTime = System.nanoTime();
    while (!requestGCDeleteStart()) {
      try {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastLogTime) > WAIT_LOG_THRESHOLD) {
          logger.info("Waited " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                      + "ms to start inline DGC.");
          lastLogTime = System.nanoTime();
        }
        wait(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return out.print(getClass().getName()).print("[").print(this.state).print("]");
  }
}
