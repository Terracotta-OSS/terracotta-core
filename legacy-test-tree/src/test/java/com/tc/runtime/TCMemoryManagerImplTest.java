/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.runtime.cache.CacheMemoryEventType;
import com.tc.runtime.cache.CacheMemoryEventsListener;
import com.tc.runtime.cache.CacheMemoryManagerEventGenerator;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Vector;

public class TCMemoryManagerImplTest extends TCTestCase implements CacheMemoryEventsListener {

  int                  usedThreshold         = 70;
  int                  usedCriticalThreshold = 90;
  long                 sleepInterval         = 50;
  int                  lc                    = 2;
  SynchronizedInt      callCount             = new SynchronizedInt(0);

  Vector               v                     = new Vector();
  private int          lastCall;
  private boolean      lastIsBelowThreshold  = false;
  private final Vector errors                = new Vector();

  public void test() throws Throwable {
    TCMemoryManager mm = new TCMemoryManagerImpl(sleepInterval, lc, true,
                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                     .getLogger(TCMemoryManagerImplTest.class))), true);

    new CacheMemoryManagerEventGenerator(usedThreshold, usedCriticalThreshold, lc, mm, this);
    try {
      hogMemory();
    } catch (Throwable e) {
      System.err.println("Got Exception : " + e);
      printStats();
      e.printStackTrace();
      throw e;
    }
    assertTrue(callCount.get() > 0);
    if (errors.size() > 0) {
      System.err.println("Errors present in the run : " + errors.size());
      System.err.println("Errors = " + errors);
      Throwable t = (Throwable) errors.get(0);
      throw t;
    }
  }

  private void printStats() {
    System.err.println("Vector size = " + v.size());
    Runtime r = Runtime.getRuntime();
    System.err.println("Memory details = Max = " + r.maxMemory() + " Free =" + r.freeMemory());
  }

  private void hogMemory() {
    for (int i = 1; i < 500000; i++) {
      byte[] b = new byte[10240];
      v.add(b);
      if (i % 10000 == 0) {
        System.err.println("Created " + i + " byte arrays - currently in vector = " + v.size());
      }
      if (i % 50 == 0) {
        ThreadUtil.reallySleep(1);
      }
    }
  }

  public void memoryUsed(CacheMemoryEventType type, MemoryUsage usage) {
    int usedPercentage = usage.getUsedPercentage();
    if (callCount.increment() % 10 == 1 || type == CacheMemoryEventType.ABOVE_CRITICAL_THRESHOLD) {
      System.err.println("Current used memory  % : " + usedPercentage + " vector size  = " + v.size());
    }

    if (this.usedThreshold > usedPercentage) {
      if (type != CacheMemoryEventType.BELOW_THRESHOLD) {
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

    if (type == CacheMemoryEventType.ABOVE_CRITICAL_THRESHOLD && this.usedCriticalThreshold > usedPercentage) {
      errors.add(new AssertionError("Received CRITICAL event with used < critical threshold : " + usedPercentage
                                    + " < " + this.usedCriticalThreshold));
    } else if (type == CacheMemoryEventType.ABOVE_THRESHOLD && this.usedCriticalThreshold < usedPercentage) {
      errors.add(new AssertionError("Received NORMAL event with used > critical threshold : " + usedPercentage + " > "
                                    + this.usedCriticalThreshold));
    } else {
      this.lastCall = 0;
    }

    if (type == CacheMemoryEventType.ABOVE_THRESHOLD) {
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
    } else if (used > 90) {
      v.clear();
      return;
    }
    int percentToDelete = (100 - used) * 4;
    synchronized (v) {
      int toRemove = Math.min(v.size() * percentToDelete / 100, v.size());
      for (int i = 0; i < toRemove; i++) {
        v.remove(v.size() - 1);
      }
    }
  }

}
