/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.UnsafeUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionObjectWrapper implements Condition, java.io.Serializable {
  public static final String CLASS_SLASH   = "com/tcclient/util/ConditionObjectWrapper";
  public static final String CLASS_DOTS    = "com.tcclient.util.ConditionObjectWrapper";
  private static final int   SIGNALLED     = 1;
  private static final int   NOT_SIGNALLED = 0;

  private ReentrantLock      originalLock;
  private Condition          realCondition;
  private int                signal        = NOT_SIGNALLED;

  private static long getSystemNanos() {
    try {
      Method m = System.class.getDeclaredMethod("nanoTime", new Class[] {});
      Object returnValue = m.invoke(null, new Object[] {});
      return ((Long) returnValue).longValue();
    } catch (NoSuchMethodException e) {
      throw new AssertionError("ReentrantLockUtils was executed in a pre-1.5 jdk environment.");
    } catch (IllegalAccessException e) {
      throw new AssertionError("ReentrantLockUtils was executed in a pre-1.5 jdk environment.");
    } catch (IllegalArgumentException e) {
      throw new AssertionError("ReentrantLockUtils was executed in a pre-1.5 jdk environment.");
    } catch (InvocationTargetException e) {
      throw new AssertionError("ReentrantLockUtils was executed in a pre-1.5 jdk environment.");
    }
  }

  public ConditionObjectWrapper(Condition realCondition, ReentrantLock originalLock) {
    this.originalLock = originalLock;
    this.realCondition = realCondition;
  }

  private void fullRelease() {
    if (originalLock.getHoldCount() > 0) {
      while (originalLock.getHoldCount() > 0) {
        UnsafeUtil.monitorExit(originalLock);
        ManagerUtil.monitorExit(originalLock);
      }
    } else { // The else part is needed only when the await of the Condition object is executed
             // in an applicator as ManagerUtil.monitorEnter() is short circuited in applicator.
      while (Thread.holdsLock(originalLock)) {
        UnsafeUtil.monitorExit(originalLock);
      }
    }
  }

  private void reacquireLock(int numOfHolds) {
    if (originalLock.getHoldCount() >= numOfHolds) { return; }
    while (originalLock.getHoldCount() < numOfHolds) {
      ManagerUtil.monitorEnter(originalLock, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(originalLock);
    }
  }

  private void checkCauseAndThrowInterruptedExceptionIfNecessary(TCRuntimeException e) throws InterruptedException {
    if (e.getCause() instanceof InterruptedException) {
      throw (InterruptedException) e.getCause();
    } else {
      throw e;
    }
  }

  private void checkCauseAndIgnoreInterruptedException(TCRuntimeException e) {
    if (!(e.getCause() instanceof InterruptedException)) { throw e; }
  }

  public void await() throws InterruptedException {
    if (ManagerUtil.isManaged(originalLock)) {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      if (Thread.interrupted()) { throw new InterruptedException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        synchronized (realCondition) {
          fullRelease();
          realCondition.wait();
        }
      } catch (TCRuntimeException e) {
        checkCauseAndThrowInterruptedExceptionIfNecessary(e);
      } finally {
        reacquireLock(numOfHolds);
      }

      if (Thread.interrupted()) { throw new InterruptedException(); }
    } else {
      realCondition.await();
    }
  }

  public void awaitUninterruptibly() {
    if (ManagerUtil.isManaged(originalLock)) {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        synchronized (realCondition) {
          fullRelease();
          signal = NOT_SIGNALLED;
          while (signal == NOT_SIGNALLED) {
            try {
              realCondition.wait();
            } catch (TCRuntimeException e) {
              checkCauseAndIgnoreInterruptedException(e);
            } catch (InterruptedException e) {
              // ignoring interrupt;
            }
          }
        }
      } finally {
        reacquireLock(numOfHolds);
      }

      Thread.interrupted(); // Clear interrupted flag if the thread is interrupted during wait.
    } else {
      realCondition.awaitUninterruptibly();
    }

  }

  public long awaitNanos(long nanosTimeout) throws InterruptedException {
    if (ManagerUtil.isManaged(originalLock)) {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      if (Thread.interrupted()) { throw new InterruptedException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        synchronized (realCondition) {
          fullRelease();
          long startTime = getSystemNanos();
          TimeUnit.NANOSECONDS.timedWait(realCondition, nanosTimeout);
          long remainingTime = nanosTimeout - (getSystemNanos() - startTime);
          return remainingTime;
        }
      } catch (TCRuntimeException e) {
        checkCauseAndThrowInterruptedExceptionIfNecessary(e);
        return 0;
      } finally {
        reacquireLock(numOfHolds);
      }
    } else {
      return realCondition.awaitNanos(nanosTimeout);
    }
  }

  public boolean await(long time, TimeUnit unit) throws InterruptedException {
    if (ManagerUtil.isManaged(originalLock)) {
      if (unit == null) { throw new NullPointerException(); }
      return awaitNanos(unit.toNanos(time)) > 0;
    } else {
      return realCondition.await(time, unit);
    }
  }

  public boolean awaitUntil(Date deadline) throws InterruptedException {
    if (ManagerUtil.isManaged(originalLock)) {
      if (deadline == null) { throw new NullPointerException(); }

      long abstime = deadline.getTime();
      if (System.currentTimeMillis() > abstime) { return true; }
      return !await(abstime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    } else {
      return realCondition.awaitUntil(deadline);
    }
  }

  public void signal() {
    if (ManagerUtil.isManaged(originalLock)) {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      synchronized (realCondition) {
        realCondition.notify();
        signal = SIGNALLED;
      }
    } else {
      realCondition.signal();
    }

  }

  public void signalAll() {
    if (ManagerUtil.isManaged(originalLock)) {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      synchronized (realCondition) {
        realCondition.notifyAll();
        signal = SIGNALLED;
      }
    } else {
      realCondition.signalAll();
    }
  }

  public int getWaitQueueLength() {
    if (realCondition == null) throw new NullPointerException();
    return ManagerUtil.waitLength(realCondition);
  }
}
