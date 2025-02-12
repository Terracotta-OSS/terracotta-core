/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.services;

import com.tc.net.utils.L2Utils;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.tc.util.Assert;
import org.terracotta.server.ServerEnv;


/**
 * A basic utility class which allows tasks to be scheduled to be run, in a background thread, at some delayed point in the
 *  future.
 * NOTE:  This timer is single-threaded as it is expected that all tasks it runs are small.
 */
public class SingleThreadedTimer implements ISimpleTimer {
  private final TimeSource timeSource;
  private final Thread timerThread;
  private boolean threadIsRunning = true;
  private boolean threadIsStarted = false;
  private long nextId = 1L;
  private final PriorityQueue<ListElement> queue = new PriorityQueue<>(1, new Comparator<ListElement>(){
    @Override
    public int compare(ListElement o1, ListElement o2) {
      long delta = o1.startTimeMillis - o2.startTimeMillis;
      return (0 == delta)
          ? 0
          : ((delta < 0) ? -1 : 1);
    }});
  private boolean isInPoke = false;

  public SingleThreadedTimer(TimeSource source, ThreadGroup group) {
    this.timeSource = (null != source) ? source : new TimeSource() {
      @Override
      public long currentTimeMillis() {
        return System.currentTimeMillis();
      }
    };
    timerThread = new Thread(group, () -> {
      Runnable nextRunnable = SingleThreadedTimer.this.getNextRunnable();
      while (null != nextRunnable) {
        try {
          nextRunnable.run();
        } catch (Throwable t) {
          System.err.println("ERROR:  Unexpected exception in timer (timed events may be dropped)");
          t.printStackTrace();
        }
        nextRunnable = SingleThreadedTimer.this.getNextRunnable();
      }
    }, ServerEnv.getServer().getIdentifier() + " - SingleThreadedTimer");
  }

  @Override
  public synchronized void start() {
    if (this.threadIsRunning && !this.threadIsStarted) {
      this.threadIsStarted = true;
      this.timerThread.setDaemon(true);
      this.timerThread.start();
    }
  }

  @Override
  public void stop() throws InterruptedException {
    synchronized (this) {
      this.threadIsRunning = false;
      this.notifyAll();
    }
    this.timerThread.join();
  }

  public synchronized void poke() {
    this.isInPoke = true;
    while (this.isInPoke) {
      this.notifyAll();
      try {
        this.wait();
      } catch (InterruptedException e) {
        L2Utils.handleInterrupted(null, e);
      }
    }
  }

  @Override
  public long currentTimeMillis() {
    return this.timeSource.currentTimeMillis();
  }

  @Override
  public synchronized long addDelayed(Runnable toRun, long startTimeMillis) {
    Assert.assertNotNull(toRun);
    return enqueueNewElement(toRun, startTimeMillis, 0L);
  }

  @Override
  public synchronized long addPeriodic(Runnable toRun, long startTimeMillis, long repeatPeriodMillis) {
    Assert.assertNotNull(toRun);
    Assert.assertTrue(repeatPeriodMillis > 0);
    return enqueueNewElement(toRun, startTimeMillis, repeatPeriodMillis);
  }

  @Override
  public synchronized boolean cancel(long id) {
    // Note that, if this is a hot-point, we could change it to be a side Map which points into the list but this keeps things simple and the list is currently always short.
    boolean didCancel = false;
    Iterator<ListElement> iterator = this.queue.iterator();
    while (!didCancel && iterator.hasNext()) {
      ListElement next = iterator.next();
      if (next.id == id) {
        iterator.remove();
        didCancel = true;
      }
    }
    return didCancel;
  }

  public synchronized void cancelAll() {
    this.queue.clear();
  }


  private synchronized Runnable getNextRunnable() {
    Runnable toRun = null;
    while (this.threadIsRunning && (null == toRun)) {
      long millisToSleep = 0;
      ListElement head = this.queue.peek();
      if (null != head) {
        long now = this.timeSource.currentTimeMillis();
        if (head.startTimeMillis <= now) {
          // We will run this.
          toRun = head.toRun;
          this.queue.remove();
          // See if we want to discard this or re-enqueue it.
          if (head.periodTimeMillis > 0) {
            ListElement newElement = new ListElement(head.id, head.toRun, head.startTimeMillis + head.periodTimeMillis, head.periodTimeMillis);
            this.queue.add(newElement);
          }
        } else {
          // We will sleep.
          millisToSleep = head.startTimeMillis - now;
        }
      }
      if (null == toRun) {
        // This will cause us to sleep until the next event, unless someone notifies us, first.
        if (this.isInPoke) {
          this.isInPoke = false;
          this.notifyAll();
        }
        try {
          this.wait(millisToSleep);
        } catch (InterruptedException e) {
          L2Utils.handleInterrupted(null, e);
        }
      }
    }
    return toRun;
  }

  private long enqueueNewElement(Runnable toRun, long startTimeMillis, long repeatPeriodMillis) {
    long id = this.nextId;
    this.nextId += 1;
    ListElement element = new ListElement(id, toRun, startTimeMillis, repeatPeriodMillis);
    this.queue.add(element);
    this.notifyAll();
    
    return id;
  }


  private static class ListElement {
    public final long id;
    public final Runnable toRun;
    public final long startTimeMillis;
    public final long periodTimeMillis;
    
    public ListElement(long id, Runnable toRun, long startTimeMillis, long periodTimeMillis) {
      this.id = id;
      this.toRun = toRun;
      this.startTimeMillis = startTimeMillis;
      this.periodTimeMillis = periodTimeMillis;
    }
  }

}
