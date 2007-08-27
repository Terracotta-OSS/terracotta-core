/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.util.concurrent.locks;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.UnsafeUtil;
import com.tc.util.concurrent.locks.TCLock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ConditionObject implements Condition, java.io.Serializable {
  private transient List      waitingThreads;
  private transient int       numOfWaitingThreards;
  private transient Map       waitOnUnshared;

  private final Lock originalLock;
  private final SyncCondition realCondition;

  private static long getSystemNanos() {
    return System.nanoTime();
  }

  public ConditionObject(TCLock originalLock) {
    this.originalLock = originalLock;
    this.realCondition = new SyncCondition();
    this.waitingThreads = new ArrayList();
    this.numOfWaitingThreards = 0;
    this.waitOnUnshared = new HashMap();
  }

  public ConditionObject() {
    this.originalLock = null;
    this.realCondition = null;
    this.waitingThreads = new ArrayList();
    this.numOfWaitingThreards = 0;
    this.waitOnUnshared = new HashMap();
  }

  private void fullRelease() {
    while (((TCLock)originalLock).localHeldCount() > 0) {
      originalLock.unlock();
    }
  }

  private void reacquireLock(int numOfHolds) {
    if (((TCLock)originalLock).localHeldCount() >= numOfHolds) { return; }
    while (((TCLock)originalLock).localHeldCount() < numOfHolds) {
      originalLock.lock();
    }
  }

  private void checkCauseAndThrowInterruptedExceptionIfNecessary(TCRuntimeException e) throws InterruptedException {
    if (e.getCause() instanceof InterruptedException) {
      throw (InterruptedException) e.getCause();
    } else {
      throw e;
    }
  }

  private void addWaitOnUnshared() {
    waitOnUnshared.put(Thread.currentThread(), ManagerUtil.isManaged(realCondition) ? Boolean.FALSE : Boolean.TRUE);
  }

  private boolean isLockRealConditionInUnshared() {
    if (!ManagerUtil.isManaged(realCondition) || !ManagerUtil.isHeldByCurrentThread(realCondition, LockLevel.WRITE)) { return true; }
    return false;
  }

  public void await() throws InterruptedException {
    Thread currentThread = Thread.currentThread();

    if (!((TCLock)originalLock).isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
    if (Thread.interrupted()) { throw new InterruptedException(); }

    int numOfHolds = ((TCLock)originalLock).localHeldCount();

    realCondition.incrementVersionIfSignalled();
    int version = realCondition.getVersion();
    fullRelease();
    try {
      ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(realCondition);
      boolean isLockInUnshared = isLockRealConditionInUnshared();
      try {
        if (realCondition.hasNotSignalledOnVersion(version)) {
          waitingThreads.add(currentThread);
          numOfWaitingThreards++;

          addWaitOnUnshared();
          try {
            ManagerUtil.objectWait0(realCondition);
          } finally {
            waitOnUnshared.remove(currentThread);
            waitingThreads.remove(currentThread);
            numOfWaitingThreards--;
          }
        }
      } finally {
        UnsafeUtil.monitorExit(realCondition);
        if (!isLockInUnshared) {
          ManagerUtil.monitorExit(realCondition);
        }
      }
    } catch (TCRuntimeException e) {
      checkCauseAndThrowInterruptedExceptionIfNecessary(e);
    } finally {
      reacquireLock(numOfHolds);
    }
  }

  public void awaitUninterruptibly() {
    Thread currentThread = Thread.currentThread();

    if (!((TCLock)originalLock).isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

    int numOfHolds = ((TCLock)originalLock).localHeldCount();
    boolean isInterrupted = false;
    realCondition.incrementVersionIfSignalled();
    int version = realCondition.getVersion();
    fullRelease();
    try {
      ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(realCondition);
      boolean isLockInUnshared = isLockRealConditionInUnshared();
      try {
        if (realCondition.hasNotSignalledOnVersion(version)) {
          while (true) {
            waitingThreads.add(currentThread);
            numOfWaitingThreards++;

            addWaitOnUnshared();
            try {
              ManagerUtil.objectWait0(realCondition);
              break;
            } catch (InterruptedException e) {
              isInterrupted = true;
            } finally {
              waitOnUnshared.remove(currentThread);
              waitingThreads.remove(currentThread);
              numOfWaitingThreards--;
            }
          }
        }
      } finally {
        UnsafeUtil.monitorExit(realCondition);
        if (!isLockInUnshared) {
          ManagerUtil.monitorExit(realCondition);
        }
      }
    } finally {
      reacquireLock(numOfHolds);
    }

    if (isInterrupted) {
      currentThread.interrupt();
    }
  }

  public long awaitNanos(long nanosTimeout) throws InterruptedException {
    Thread currentThread = Thread.currentThread();

    if (!((TCLock)originalLock).isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
    if (Thread.interrupted()) { throw new InterruptedException(); }

    int numOfHolds = ((TCLock)originalLock).localHeldCount();
    realCondition.incrementVersionIfSignalled();
    int version = realCondition.getVersion();
    fullRelease();
    try {
      ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(realCondition);
      boolean isLockInUnshared = isLockRealConditionInUnshared();
      try {
        if (realCondition.hasNotSignalledOnVersion(version)) {
          waitingThreads.add(currentThread);
          numOfWaitingThreards++;

          addWaitOnUnshared();
          try {
            long startTime = getSystemNanos();
            TimeUnit.NANOSECONDS.timedWait(realCondition, nanosTimeout);
            long remainingTime = nanosTimeout - (getSystemNanos() - startTime);
            return remainingTime;
          } finally {
            waitOnUnshared.remove(currentThread);
            waitingThreads.remove(currentThread);
            numOfWaitingThreards--;
          }
        } else {
          return nanosTimeout;
        }
      } finally {
        UnsafeUtil.monitorExit(realCondition);
        if (!isLockInUnshared) {
          ManagerUtil.monitorExit(realCondition);
        }
      }
    } catch (TCRuntimeException e) {
      checkCauseAndThrowInterruptedExceptionIfNecessary(e);
      return 0;
    } finally {
      reacquireLock(numOfHolds);
    }
  }

  public boolean await(long time, TimeUnit unit) throws InterruptedException {
    if (unit == null) { throw new NullPointerException(); }
    return awaitNanos(unit.toNanos(time)) > 0;
  }

  public boolean awaitUntil(Date deadline) throws InterruptedException {
    if (deadline == null) { throw new NullPointerException(); }

    long abstime = deadline.getTime();
    if (System.currentTimeMillis() > abstime) { return true; }
    return !await(abstime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  private boolean hasWaitOnUnshared() {
    return waitOnUnshared.values().contains(Boolean.TRUE);
  }

  public void signal() {
    if (!((TCLock)originalLock).isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

    ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
    UnsafeUtil.monitorEnter(realCondition);
    boolean isLockInUnshared = isLockRealConditionInUnshared();
    try {
      ManagerUtil.objectNotify(realCondition);
      if (hasWaitOnUnshared()) {
        realCondition.notify();
      }
      realCondition.setSignalled();
    } finally {
      UnsafeUtil.monitorExit(realCondition);
      if (!isLockInUnshared) {
        ManagerUtil.monitorExit(realCondition);
      }
    }
  }

  public void signalAll() {
    if (!((TCLock)originalLock).isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

    ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
    UnsafeUtil.monitorEnter(realCondition);
    boolean isLockInUnshared = isLockRealConditionInUnshared();
    try {
      ManagerUtil.objectNotifyAll(realCondition);
      if (hasWaitOnUnshared()) {
        realCondition.notifyAll();
      }
      realCondition.setSignalled();
    } finally {
      UnsafeUtil.monitorExit(realCondition);
      if (!isLockInUnshared) {
        ManagerUtil.monitorExit(realCondition);
      }
    }
  }

  public int getWaitQueueLength(Lock lock) {
    if (originalLock != lock) throw new IllegalArgumentException("not owner");
    if (!ManagerUtil.isManaged(originalLock)) {
      return numOfWaitingThreards;
    } else {
      return ManagerUtil.waitLength(realCondition);
    }
  }

  public Collection getWaitingThreads(Lock lock) {
    if (originalLock != lock) throw new IllegalArgumentException("not owner");
    return waitingThreads;
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    this.waitingThreads = new ArrayList();
    this.numOfWaitingThreards = 0;
    this.waitOnUnshared = new HashMap();
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();
  }

  public static class SyncCondition implements java.io.Serializable {
    private final static byte SIGNALLED     = 0;
    private final static byte NOT_SIGNALLED = 1;

    private int               version;
    private byte              signalled;

    public SyncCondition() {
      super();
      this.version = 0;
      this.signalled = NOT_SIGNALLED;
    }

    public boolean isSignalled() {
      return signalled == SIGNALLED;
    }

    public void setSignalled() {
      signalled = SIGNALLED;
    }

    public void incrementVersionIfSignalled() {
      if (isSignalled()) {
        this.version++;
        resetSignalled();
      }
    }

    public int getVersion() {
      return this.version;
    }

    public boolean hasNotSignalledOnVersion(int targetVersion) {
      return !isSignalled() && (this.version == targetVersion);
    }

    private void resetSignalled() {
      this.signalled = NOT_SIGNALLED;
    }
  }

}
