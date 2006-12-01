/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tc.util.Assert;

import java.util.LinkedList;
import java.util.List;

/**
 * @ThreadSafe
 * @LivenessTuned
 */
final class MonitoredWorkQueue {

  private static final int TIMEOUT      = 5000;
  private final Object[]   queue;
  private final long[]     intervalStartTime;
  private final List       points;
  private int              count, first, last;
  private long             intervalMagnitude;
  private final Object     intervalLock = new Object();
  private volatile boolean overflow;
  private final int        quarterCapacity;
  private final boolean    keepMetronome;
  
  // allows server to catch up when blocking for 2min at saturation point
  private int              waitCount;

  MonitoredWorkQueue(int size, boolean keepMetronome) {
    Assert.assertTrue(size > 0);
    this.queue = new Object[size];
    this.intervalStartTime = new long[size];
    this.points = new LinkedList();
    this.keepMetronome = keepMetronome;
    this.quarterCapacity = size / 4;
  }

  synchronized void put(Object obj) {
    if (count == quarterCapacity) {
      System.err.println("WORK QUEUE HAS REACHED 25% CAPACITY - NOW WOULD BE A GOOD TIME FOR A THREAD DUMP");
      if (waitCount++ < 2) {
        System.err.println("PAUSE (2 seconds)");
        try {
          Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
    if (count == queue.length) {
      overflow = true;
      while (true)
        try {
          wait();
        } catch (InterruptedException e) {
          // ignore
        }
    }

    boolean startTimer = false;
    if (obj instanceof Metronome) {
      if (!keepMetronome) obj = ((Metronome) obj).object;
      startTimer = true;
    }

    queue[last] = obj;
    last = (last + 1) % queue.length;
    count++;
    if (startTimer) intervalStartTime[last] = System.nanoTime();
    notifyAll();
  }

  synchronized Object get() throws InterruptedException, WorkQueueOverflowException {
    if (overflow) throw new WorkQueueOverflowException();
    long waitTimeout;
    // timeout is more efficient than a poison pill
    while (count == 0) {
      waitTimeout = System.currentTimeMillis();
      wait(TIMEOUT);
      if ((System.currentTimeMillis() - waitTimeout) >= TIMEOUT) return null; // detect completion
    }

    Object obj = queue[first];
    queue[first] = null;
    count--;
    first = (first + 1) % queue.length;

    // manage measurements
    if (intervalStartTime[first] != 0L) {
      long waitTime = System.nanoTime() - intervalStartTime[first];
      synchronized (points) {
        points.add(new Measurement(intervalMagnitude, waitTime));
      }
      synchronized (intervalLock) {
        if (keepMetronome) ((Metronome) obj).load = intervalMagnitude;
        intervalMagnitude = 0;
        intervalStartTime[first] = 0L;
      }
    }
    synchronized (intervalLock) {
      intervalMagnitude++;
    }

    return obj;
  }

  Measurement[] getWaitTimes() {
    synchronized (points) {
      return (Measurement[]) points.toArray(new Measurement[0]);
    }
  }
}
