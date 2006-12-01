/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Vector;

public class TCMemoryManagerImplTest extends TCTestCase implements MemoryEventsListener {

  int             usedThreshold         = 70;
  int             usedCriticalThreshold = 90;
  long            sleepInterval         = 50;
  int             lc                    = 2;
  SynchronizedInt callCount             = new SynchronizedInt(0);

  Vector          v                     = new Vector();
  private int     lastCall;
  private boolean lastIsBelowThreshold  = false;
  private Vector  errors                = new Vector();

  public void test() throws Throwable {
    TCMemoryManager mm = new TCMemoryManagerImpl(usedThreshold, usedCriticalThreshold, sleepInterval, lc, true);
    mm.registerForMemoryEvents(this);
    hogMemory();
    assertTrue(callCount.get() > 0);
    if (errors.size() > 0) {
      System.err.println("Errors present in the run : " + errors.size());
      System.err.println("Errors = " + errors);
      Throwable t = (Throwable) errors.get(0);
      throw t;
    }
  }

  private void hogMemory() {
    for (int i = 1; i < 500000; i++) {
      byte[] b = new byte[10240];
      v.add(b);
      if (i % 10000 == 0) {
        System.err.println("Created " + i + " byte arrays - currently in vector = " + v.size());
      }
      if (i % 50 == 0) {
        ThreadUtil.reallySleep(0, 10);
      }
    }
  }

  public void memoryUsed(MemoryEventType type, MemoryUsage usage) {
    int usedPercentage = usage.getUsedPercentage();
    if (callCount.increment() % 10 == 1 || type == MemoryEventType.ABOVE_CRITICAL_THRESHOLD) {
      System.err.println("Current used memory  % : " + usedPercentage + " vector size  = " + v.size());
    }

    if (this.usedThreshold > usedPercentage) {
      if (type != MemoryEventType.BELOW_THRESHOLD) {
        errors.add(new AssertionError("Used Percentage reported (" + usedPercentage + ") is less than Used threshold ("
                                      + usedThreshold + ") set, but type is " + type));
      } else if (lastIsBelowThreshold) {
        errors.add(new AssertionError(type + " is reported more often than it should be. Used % is " + usedPercentage));
      }
      lastIsBelowThreshold = true;
      this.lastCall = 0;
    } else {
      lastIsBelowThreshold = false;
    }

    if (type == MemoryEventType.ABOVE_CRITICAL_THRESHOLD && this.usedCriticalThreshold > usedPercentage) {
      errors.add(new AssertionError("Received CRITICAL event with used < critical threshold : " + usedPercentage
                                    + " < " + this.usedCriticalThreshold));
    } else if (type == MemoryEventType.ABOVE_THRESHOLD && this.usedCriticalThreshold < usedPercentage) {
      errors.add(new AssertionError("Received NORMAL event with used > critical threshold : " + usedPercentage + " > "
                                    + this.usedCriticalThreshold));
    } else {
      this.lastCall = 0;
    }

    if (type == MemoryEventType.ABOVE_THRESHOLD) {
      if (this.lastCall == usedPercentage) {
        errors.add(new AssertionError("Recd two callbacks with same value (" + usedPercentage + ")"));
      } else if (Math.abs(this.lastCall - usedPercentage) < lc) {
        errors.add(new AssertionError("Recd two callbacks with  values less that least count (" + usedPercentage
                                      + " , " + lastCall + ") - LC = " + lc));
      }
      this.lastCall = usedPercentage;
    }
    releaseSomeMemory(usedPercentage);
  }

  // Releases 10 % of the elements in the vector for 70 % used
  private void releaseSomeMemory(int used) {
    if (used < usedThreshold) {
      return;
    } else if (used > 97) {
      v.clear();
      return;
    }
    int percentToDelete = (100 - usedThreshold) * 10 / (100 - used);
    synchronized (v) {
      int toRemove = Math.min(v.size() * percentToDelete / 100, v.size());
      // if (callCount.get() % 10 == 1) {
      // System.err.println("Clearing " + toRemove + " of " + v.size() + " ie " + percentToDelete + " %");
      //      }
      for (int i = 0; i < toRemove; i++) {
        v.remove(v.size() - 1);
      }
    }
  }

}
