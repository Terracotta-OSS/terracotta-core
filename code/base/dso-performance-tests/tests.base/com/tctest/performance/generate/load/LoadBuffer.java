/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ThreadSafe
 * @LivenessTuned
 */
final class LoadBuffer {

  private static final long NANOSEC = 1000000000L;
  private final Object[]    buffer;
  private final boolean[]   isMetronome;
  private int               in, out, count;
  private final LatchTimer  latch;
  private volatile boolean  beginClockCycle;
  private volatile boolean  illegalState;
  private long              clock, nanotime;

  LoadBuffer(int size) {
    Assert.assertTrue(size > 0);
    buffer = new Object[size];
    isMetronome = new boolean[size];
    latch = new LatchTimer(this);
    latch.start();
  }

  synchronized boolean put(Object obj, boolean metronome) throws InterruptedException {
    while (count == buffer.length)
      wait();

    if (illegalState) return false;

    buffer[in] = obj;
    isMetronome[in] = metronome;
    ++count;
    in = (in + 1) % buffer.length;
    notifyAll();
    return true;
  }

  synchronized Object get() throws InterruptedException {
    pageClock();
    while (count == 0 || !latch.isOpen || (isMetronome[out] && !beginClockCycle))
      wait();

    if (beginClockCycle && !isMetronome[out]) latch.adjustIntervals();
    beginClockCycle = false;

    Object obj = buffer[out];
    buffer[out] = null;

    if (isMetronome[out]) {
      Metronome metronome = new Metronome(obj);
      obj = metronome;
      isMetronome[out] = false;
    }

    --count;
    out = (out + 1) % buffer.length;
    notifyAll();

    return obj;
  }
  
  private void pageClock() {
    if ((nanotime = System.nanoTime()) > clock) {
      clock = nanotime + NANOSEC;
      beginClockCycle = true;
    }
  }

  private class LatchTimer extends Thread {

    private static final int    DELAY         = 1000;
    private static final int    RATIO         = 100;
    private final Object        parent;
    private final AtomicInteger openInterval, closeInterval, intervalCount;
    private volatile boolean    isOpen        = false;
    private int                 intervalRatio = 11;

    LatchTimer(Object parent) {
      setDaemon(true);
      setPriority(Thread.MAX_PRIORITY);
      this.parent = parent;
      openInterval = new AtomicInteger(0);
      closeInterval = new AtomicInteger(0);
      intervalCount = new AtomicInteger(0);
      adjustIntervals();
    }

    private void adjustIntervals() {
      System.err.println("Adjusting Load Buffer Latch Intervals (" + (intervalRatio - 1) + ")");
      if (--intervalRatio == 1) {
        System.err.println("Load Buffer Latch Open, CPU Bound");
        openInterval.getAndSet(DELAY);
      } else if (intervalRatio == 0) {
        illegalState = true;
        isOpen = false;
        return;
      }
      closeInterval.getAndSet(new Double(Math.floor(intervalRatio * DELAY / (2 * RATIO))).intValue());
      openInterval.getAndSet(new Double(Math.floor((DELAY / intervalRatio) - closeInterval.intValue())).intValue());
      intervalCount.getAndSet(0);
    }

    public void run() {
      while (!illegalState) {
        try {
          isOpen = true;
          if ((intervalCount.incrementAndGet() * RATIO) == DELAY) {
            intervalCount.getAndSet(0);
          }
          synchronized (parent) {
            pageClock();
            parent.notifyAll();
          }
          Thread.sleep(openInterval.intValue());

          if (openInterval.intValue() != DELAY) {
            isOpen = false;
            synchronized (parent) {
              parent.notifyAll();
            }
            Thread.sleep(closeInterval.intValue());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      if (illegalState) isOpen = false;
    }
  }
}
