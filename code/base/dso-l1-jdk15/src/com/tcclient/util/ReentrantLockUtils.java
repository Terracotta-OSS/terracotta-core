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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockUtils {
  public static final String  CLASS_SLASH = "com/tcclient/util/ReentrantLockUtils";
  private static final Object waitObject  = new Object();

  private static void acquireDSOWriteLock(Object lockObject) {
    ManagerUtil.monitorEnter(lockObject, LockLevel.WRITE);
    UnsafeUtil.monitorEnter(lockObject);
  }

  private static boolean tryAcquireDSOWriteLock(Object lockObject) {
    boolean isLocked = ManagerUtil.tryMonitorEnter(lockObject, LockLevel.WRITE);
    if (isLocked) {
      UnsafeUtil.monitorEnter(lockObject);
    }
    return isLocked;
  }

  public static void lock(ReentrantLock lock) {
    acquireDSOWriteLock(lock);
  }

  public static void unlock(ReentrantLock lock) {
    if (!ManagerUtil.isCreationInProgress() && !lock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
    UnsafeUtil.monitorExit(lock);
    ManagerUtil.monitorExit(lock);
  }

  public static void lockInterruptibly(ReentrantLock lock) throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    try {
      lock(lock);
    } catch (TCRuntimeException e) {
      checkCauseAndThrowInterruptedExceptionIfNecessary(e);
    }
  }

  public static boolean tryLock(ReentrantLock lock) {
    if (lock.isHeldByCurrentThread()) {
      acquireDSOWriteLock(lock);
      return true;
    } else {
      return tryAcquireDSOWriteLock(lock);
    }
  }

  public static boolean tryLock(long timeout, TimeUnit unit, ReentrantLock lock)
      throws InterruptedException {
    if (unit == null || lock == null) { throw new NullPointerException(); }
    if (Thread.interrupted()) { throw new InterruptedException(); }

    if (!tryLock(lock)) {

      // wait until timeout expires, ignoring all interrupts while waiting.
      long totalTimeoutInNanos = unit.toNanos(timeout);
      long timeoutInNanos = TimeUnit.MILLISECONDS.toNanos(10);
      if (timeoutInNanos > totalTimeoutInNanos) {
        timeoutInNanos = totalTimeoutInNanos;
      }

      long startTime = getSystemNanos();
      long currentTime = startTime;
      synchronized (waitObject) {
        while (totalTimeoutInNanos > 0) {
          boolean lockAcquired = tryLock(lock);
          if (lockAcquired) return true;
          try {
            TimeUnit.NANOSECONDS.timedWait(waitObject, timeoutInNanos);
          } catch (TCRuntimeException e) {
            checkCauseAndThrowInterruptedExceptionIfNecessary(e);
          }
          currentTime = getSystemNanos();
          totalTimeoutInNanos -= (currentTime - startTime);
          startTime = currentTime;
        }
      }
      if (Thread.interrupted()) { throw new InterruptedException(); }
      return false;
    }
    return true;
  }

  public static int getWaitQueueLength(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObjectWrapper)) throw new IllegalArgumentException("not owner");
    return ((ConditionObjectWrapper) condition).getWaitQueueLength();
  }

  public static boolean hasWaiters(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObjectWrapper)) throw new IllegalArgumentException("not owner");
    return ((ConditionObjectWrapper) condition).getWaitQueueLength() > 0;
  }

  public static String toString(ReentrantLock lock) {
    return (lock.isLocked() ? (lock.isHeldByCurrentThread() ? "[Locally locked]" : "[Remotelly locked]") : "[Unlocked]");
  }

  private static long getSystemNanos() {
    try {
      Method m = System.class.getDeclaredMethod("nanoTime", new Class[0]);
      Object returnValue = m.invoke(null, new Object[0]);
      return ((Long) returnValue).longValue();
    } catch (NoSuchMethodException e) {
      throw new AssertionError("ReentrantLockUtils get execuited in a pre-1.5 jdk environment.");
    } catch (IllegalAccessException e) {
      throw new AssertionError("ReentrantLockUtils get execuited in a pre-1.5 jdk environment.");
    } catch (IllegalArgumentException e) {
      throw new AssertionError("ReentrantLockUtils get execuited in a pre-1.5 jdk environment.");
    } catch (InvocationTargetException e) {
      throw new AssertionError("ReentrantLockUtils get execuited in a pre-1.5 jdk environment.");
    }
  }

  // TODO: Hack to check if the cause of an TCRuntimeException is an InterruptedException and rethrow it.
  private static void checkCauseAndThrowInterruptedExceptionIfNecessary(TCRuntimeException e)
      throws InterruptedException {
    if (e.getCause() instanceof InterruptedException) {
      throw (InterruptedException) e.getCause();
    } else {
      throw e;
    }
  }

}
