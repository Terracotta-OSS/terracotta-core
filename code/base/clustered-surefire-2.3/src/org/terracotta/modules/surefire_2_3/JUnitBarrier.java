/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.modules.surefire_2_3;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import junit.framework.Test;
import junit.framework.TestCase;

public class JUnitBarrier {
  protected final int parties;
  
  protected boolean broken = false;
  protected int count;
  protected int resets = 0;

  public static void createBarrierAndWait(Test t) {
    int numberOfNodes;
    try {
      numberOfNodes = Integer.parseInt(System.getProperty("tc.numberOfNodes", "0"));
    } catch (Exception ex) {
      numberOfNodes = 0;
    }
    if (numberOfNodes == 0) {
      return;
    }

    String testName = t.getClass().getName();
    if (t instanceof TestCase) {
      testName = testName + ":" + ((TestCase) t).getName();
    }

    String globalLock = "@junit_test_suite_lock";

    JUnitBarrier barrier;
    ManagerUtil.beginLock(globalLock, Manager.LOCK_TYPE_WRITE);
    try {
      barrier = (JUnitBarrier) ManagerUtil.lookupOrCreateRoot("barrier:" + testName, //
          new JUnitBarrier(numberOfNodes));
    } finally {
      ManagerUtil.commitLock(globalLock, Manager.LOCK_TYPE_WRITE);
    }

    barrier.barrier();
  }

  public JUnitBarrier(int parties) {
    if (parties <= 0) {
      throw new IllegalArgumentException();
    }
    this.parties = parties;
    this.count = parties;
  }

  public int barrier() {
    return doBarrier(false, 0L);
  }

  protected synchronized int doBarrier(boolean flag, long l) {
    int i = --count;
    if (broken) {
      throw new RuntimeException("Barrier " + i + " broken");
    }
    if (Thread.interrupted()) {
      broken = true;
      notifyAll();
      throw new RuntimeException("Barier " + i + " interrupted");
    }
    if (i == 0) {
      count = parties;
      resets++;
      notifyAll();
      return 0;
    }
    if (flag && l <= 0L) {
      broken = true;
      notifyAll();
      throw new RuntimeException("Barier " + i + " timed out " + l);
    }

    int j = resets;
    long l1 = flag ? System.currentTimeMillis() : 0L;
    long l2 = l;
    do {
      do {
        try {
          wait(l2);
        } catch (InterruptedException ex) {
          if (resets == j) {
            broken = true;
            notifyAll();
            throw new RuntimeException("Barier " + i + " interrupted "
                + ex.toString());
          }
          Thread.currentThread().interrupt();
        }
        if (broken) {
          throw new RuntimeException("Barrier " + i + " broken");
        }
        if (j != resets) {
          return i;
        }
      } while (!flag);
      l2 = l - (System.currentTimeMillis() - l1);
    } while (l2 > 0L);
    broken = true;
    notifyAll();
    throw new RuntimeException("Barier " + i + " timed out " + l);
  }
}
