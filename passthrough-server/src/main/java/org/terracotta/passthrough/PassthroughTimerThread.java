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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class PassthroughTimerThread extends Thread {
  private boolean shouldRun = true;
  private final AtomicLong nextNumber = new AtomicLong(1L);
  private final List<ListElement> queue = new ArrayList<ListElement>();

  @Override
  public void run() {
    boolean keepRunning = true;
    while (keepRunning) {
      NextIntent intent = getNextIntent();
      keepRunning = intent.keepRunning;
      if (keepRunning) {
        Runnable toRun = intent.toRun;
        if (null != toRun) {
          try {
            toRun.run();
          } catch (Throwable t) {
            System.err.println("Unexpected exception in timer thread (timed events may be dropped)");
            t.printStackTrace();
          }
        } else {
          synchronized (this) {
            try {
              this.wait(intent.millisToSleep);
            } catch (InterruptedException e) {
              // We never interrupt this, internally.
              Assert.unexpected(e);
            }
          }
        }
      }
    }
  }

  public synchronized void shutdown() {
    this.shouldRun = false;
    this.notifyAll();
  }

  public synchronized long scheduleAfterDelay(Runnable runnable, long millisBeforeSend) {
    long currentTime = System.currentTimeMillis();
    long nextId = this.nextNumber.getAndIncrement();
    ListElement element = new ListElement(nextId, runnable, currentTime + millisBeforeSend, 0);
    enqueueInList(element);
    return nextId;
  }

  public synchronized long schedulePeriodically(Runnable runnable, long millisBetweenSends) {
    long currentTime = System.currentTimeMillis();
    long nextId = this.nextNumber.getAndIncrement();
    ListElement element = new ListElement(nextId, runnable, currentTime + millisBetweenSends, millisBetweenSends);
    enqueueInList(element);
    return nextId;
  }

  public synchronized void cancelMessage(long token) {
    // Walk the queue and remove this element, if we find it (it might already be gone).
    int indexToDrop = -1;
    for (int i = 0; ((-1 == indexToDrop) && (i < this.queue.size())); ++i) {
      if (this.queue.get(i).id == token) {
        indexToDrop = i;
      }
    }
    if (-1 != indexToDrop) {
      this.queue.remove(indexToDrop);
    }
  }


  private synchronized NextIntent getNextIntent() {
    long currentTime = System.currentTimeMillis();
    boolean shouldContinue = this.shouldRun;
    Runnable toRun = null;
    long millisToSleep = 0;
    if (shouldContinue) {
      if (!this.queue.isEmpty()) {
        ListElement firstInList = this.queue.get(0);
        if (firstInList.timeToRun <= currentTime) {
          this.queue.remove(0);
          toRun = firstInList.toRun;
          long reschedulePeriod = firstInList.reschedulePeriod;
          if (reschedulePeriod > 0) {
            enqueueInList(new ListElement(firstInList.id, toRun, currentTime + reschedulePeriod, reschedulePeriod));
          }
        } else {
          millisToSleep = (firstInList.timeToRun - currentTime);
        }
      }
    }
    return new NextIntent(shouldContinue, toRun, millisToSleep);
  }

  private void enqueueInList(ListElement element) {
    int listSize = this.queue.size();
    int indexToInsert = listSize;
    for (int i = 0; i < listSize; ++i) {
      if (element.timeToRun < this.queue.get(i).timeToRun) {
        indexToInsert = i;
        break;
      }
    }
    this.queue.add(indexToInsert, element);
    this.notifyAll();
  }


  private static class ListElement {
    public final long id;
    public final Runnable toRun;
    public final long timeToRun;
    public final long reschedulePeriod;
    
    public ListElement(long id, Runnable toRun, long timeToRun, long reschedulePeriod) {
      this.id = id;
      this.toRun = toRun;
      this.timeToRun = timeToRun;
      this.reschedulePeriod = reschedulePeriod;
    }
  }


  private static class NextIntent {
    public final boolean keepRunning;
    public final Runnable toRun;
    public final long millisToSleep;
    
    public NextIntent(boolean keepRunning, Runnable toRun, long millisToSleep) {
      this.keepRunning = keepRunning;
      this.toRun = toRun;
      this.millisToSleep = millisToSleep;
    }
  }
}