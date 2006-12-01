/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent.locks;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.Assert;
import com.tc.util.UnsafeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ReentrantLock implements Lock, java.io.Serializable {
  private boolean  isFair;

  /** Current owner thread */
  transient Thread owner          = null;
  transient int    numOfHolds     = 0;
  transient List   waitingQueue   = new ArrayList();
  transient int    state          = 0;
  transient int    numQueued      = 0;
  transient Stack  lockInUnShared = new Stack();

  transient Object lock           = new Object();

  public ReentrantLock() {
    this.isFair = false;

    this.owner = null;
    this.numOfHolds = 0;
    this.waitingQueue = new ArrayList();
    this.state = 0;
    this.numQueued = 0;
    this.lock = new Object();
    this.lockInUnShared = new Stack();
  }

  public ReentrantLock(boolean fair) {
    this.isFair = fair;

    this.owner = null;
    this.numOfHolds = 0;
    this.waitingQueue = new ArrayList();
    this.state = 0;
    this.numQueued = 0;
    this.lock = new Object();
    this.lockInUnShared = new Stack();
  }

  public void lock() {
    synchronized (lock) {
      while (owner != null && owner != Thread.currentThread() && lockInUnShared.contains(Boolean.TRUE)) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }

      waitingQueue.add(Thread.currentThread());
      numQueued++;
    }

    ManagerUtil.monitorEnter(this, LockLevel.WRITE);
    UnsafeUtil.monitorEnter(this);

    synchronized (lock) {
      innerSetLockState();
      waitingQueue.remove(Thread.currentThread());
      numQueued--;
    }
  }

  public void lockInterruptibly() throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    lock();
    if (Thread.interrupted()) {
      if (isHeldByCurrentThread()) {
        unlock();
      }
      throw new InterruptedException();
    }
  }

  public boolean tryLock() {
    boolean canLock = false;
    synchronized (lock) {
      canLock = canProceedToLock();
      if (ManagerUtil.isManaged(this) && canLock) {
        canLock = ManagerUtil.tryMonitorEnter(this, LockLevel.WRITE);
        if (canLock) {
          UnsafeUtil.monitorEnter(this);
        }
      } else {
        if (canLock) {
          UnsafeUtil.monitorEnter(this);
        }
      }
      if (canLock) {
        innerSetLockState();
      }
      return canLock;
    }
  }

  public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    if (!tryLock()) {
      long timeoutInNanos = TimeUnit.MICROSECONDS.toNanos(10);
      long totalTimeoutInNanos = unit.toNanos(timeout);
      long startTimeInNanos = System.nanoTime();

      if (timeoutInNanos > totalTimeoutInNanos) {
        timeoutInNanos = totalTimeoutInNanos;
      }

      synchronized (lock) {
        waitingQueue.add(Thread.currentThread());
        numQueued++;
        boolean locked = false;

        try {
          while (!locked && totalTimeoutInNanos > 0) {
            unit.timedWait(lock, timeoutInNanos);
            totalTimeoutInNanos -= (System.nanoTime() - startTimeInNanos);
            locked = tryLock();
          }
        } finally {
          waitingQueue.remove(Thread.currentThread());
          numQueued--;
        }
        return locked;
      }
    }
    return true;
  }

  public void unlock() {
    boolean needDSOUnlock = false;
    synchronized (lock) {
      boolean isLockedInUnSharedMode = ((Boolean) this.lockInUnShared.pop()).booleanValue();
      needDSOUnlock = !isLockedInUnSharedMode && ManagerUtil.isManaged(this) && !ManagerUtil.isCreationInProgress();

      if (--numOfHolds == 0) {
        owner = null;
        setState(0);
        this.lockInUnShared.remove(Thread.currentThread());
      }
      UnsafeUtil.monitorExit(this);
      if (needDSOUnlock) {
        ManagerUtil.monitorExit(this);
      } else {
        lock.notifyAll();
      }
    }
  }

  public Condition newCondition() {
    return new ConditionObject(new SyncCondition(), this);
  }

  public int getHoldCount() {
    return (owner == Thread.currentThread()) ? numOfHolds : 0;
  }

  public boolean isHeldByCurrentThread() {
    Thread owner = null;
    int state = 0;
    synchronized (lock) {
      owner = this.owner;
      state = getState();
    }
    return state != 0 && owner == Thread.currentThread();
  }

  public boolean isLocked() {
    if (ManagerUtil.isManaged(this)) {
      return isHeldByCurrentThread() || ManagerUtil.isLocked(this);
    } else {
      return getState() > 0;
    }
  }

  public final boolean isFair() {
    return isFair;
  }

  protected Thread getOwner() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return (numOfHolds == 0) ? null : owner;
    }
  }

  public final boolean hasQueuedThreads() {
    if (ManagerUtil.isManaged(this)) {
      return ManagerUtil.queueLength(this) > 0;
    } else {
      return numQueued > 0;
    }
  }

  public final boolean hasQueuedThread(Thread thread) {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      List waitingThreads = null;
      synchronized (lock) {
        waitingThreads = waitingQueue;
      }
      return waitingThreads.contains(thread);
    }
  }

  public final int getQueueLength() {
    if (ManagerUtil.isManaged(this)) {
      return ManagerUtil.queueLength(this);
    } else {
      return numQueued;
    }
  }

  protected Collection getQueuedThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      List waitingThreads = null;
      synchronized (lock) {
        waitingThreads = waitingQueue;
      }
      return waitingThreads;
    }
  }

  public boolean hasWaiters(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) throw new IllegalArgumentException(
                                                                                                               "not owner");
    return false;
  }

  public int getWaitQueueLength(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitQueueLength(this);
  }

  protected Collection getWaitingThreads(Condition condition) {
    if (ManagerUtil.isManaged(this)) throw new TCNotSupportedMethodException();

    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitingThreads(this);
  }

  private String getLockState() {
    return (isLocked() ? (isHeldByCurrentThread() ? "[Locally locked]" : "[Remotelly locked]") : "[Unlocked]");
  }

  public String toString() {
    Thread owner = null;
    return ManagerUtil.isManaged(this) ? (new StringBuilder()).append(super.toString()).append(getLockState())
        .toString() : (new StringBuilder()).append(super.toString())
        .append(
                (owner = getOwner()) != null ? (new StringBuilder()).append("[Locked by thread ")
                    .append(owner.getName()).append("]").toString() : "[Unlocked]").toString();

  }

  private boolean canProceedToLock() {
    boolean canLock = waitingQueue.isEmpty() && (getState() == 0);
    return (owner == null && canLock) || (owner == Thread.currentThread());
  }

  private void innerSetLockState() {
    if (!ManagerUtil.isManaged(ReentrantLock.this)
        || !ManagerUtil.isHeldByCurrentThread(ReentrantLock.this, LockLevel.WRITE)) {
      this.lockInUnShared.push(Boolean.TRUE);
      // isLockedInUnSharedMode = true;
    } else {
      this.lockInUnShared.push(Boolean.FALSE);
    }

    owner = Thread.currentThread();
    numOfHolds++;
    setState(1);
  }

  private void setState(int state) {
    this.state = state;
  }

  private int getState() {
    return this.state;
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    isFair = s.readBoolean();
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();
    s.writeBoolean(isFair);
  }
  
  private static class SyncCondition implements java.io.Serializable {
    public SyncCondition() {
      super();
    }
  }

  private static class ConditionObject implements Condition, java.io.Serializable {
    public static final String   CLASS_SLASH   = "com/tcclient/util/ConditionObjectWrapper";
    public static final String   CLASS_DOTS    = "com.tcclient.util.ConditionObjectWrapper";
    private static final int     SIGNALLED     = 1;
    private static final int     NOT_SIGNALLED = 0;

    private final transient List waitingThreads;
    private transient int        numOfWaitingThreards;
    private final transient Map  waitOnUnshared;

    private final ReentrantLock  originalLock;
    private final Object         realCondition;
    private int                  signal        = NOT_SIGNALLED;

    private static long getSystemNanos() {
      return System.nanoTime();
    }

    public ConditionObject(Object realCondition, ReentrantLock originalLock) {
      this.originalLock = originalLock;
      this.realCondition = realCondition;
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
      if (originalLock.getHoldCount() > 0) {
        while (originalLock.getHoldCount() > 0) {
          originalLock.unlock();
        }
      } else {
        // The else part is needed only when the await of the Condition object is executed
        // in an applicator as ManagerUtil.monitorEnter() is short circuited in applicator.
        while (Thread.holdsLock(originalLock)) {
          UnsafeUtil.monitorExit(originalLock);
        }
      }
    }

    private void reacquireLock(int numOfHolds) {
      if (originalLock.getHoldCount() >= numOfHolds) { return; }
      while (originalLock.getHoldCount() < numOfHolds) {
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

    private void checkCauseAndIgnoreInterruptedException(TCRuntimeException e) {
      if (!(e.getCause() instanceof InterruptedException)) { throw e; }
    }

    private void addWaitOnUnshared() {
      waitOnUnshared.put(Thread.currentThread(), ManagerUtil.isManaged(realCondition) ? Boolean.FALSE : Boolean.TRUE);
    }

    private boolean isLockRealConditionInUnshared() {
      if (!ManagerUtil.isManaged(realCondition) || !ManagerUtil.isHeldByCurrentThread(realCondition, LockLevel.WRITE)) { return true; }
      return false;
    }

    public void await() throws InterruptedException {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      if (Thread.interrupted()) { throw new InterruptedException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
        UnsafeUtil.monitorEnter(realCondition);
        boolean isLockInUnshared = isLockRealConditionInUnshared();
        try {
          fullRelease();

          waitingThreads.add(Thread.currentThread());
          numOfWaitingThreards++;

          addWaitOnUnshared();
          try {
            ManagerUtil.objectWait0(realCondition);
          } finally {
            waitOnUnshared.remove(Thread.currentThread());
            waitingThreads.remove(Thread.currentThread());
            numOfWaitingThreards--;
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

      if (Thread.interrupted()) { throw new InterruptedException(); }
    }

    public void awaitUninterruptibly() {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
        UnsafeUtil.monitorEnter(realCondition);
        boolean isLockInUnshared = isLockRealConditionInUnshared();
        try {
          fullRelease();
          signal = NOT_SIGNALLED;
          while (signal == NOT_SIGNALLED) {
            waitingThreads.add(Thread.currentThread());
            numOfWaitingThreards++;

            addWaitOnUnshared();
            try {
              ManagerUtil.objectWait0(realCondition);
            } catch (TCRuntimeException e) {
              checkCauseAndIgnoreInterruptedException(e);
            } catch (InterruptedException e) {
              // ignoring interrupt;
            } finally {
              waitOnUnshared.remove(Thread.currentThread());
              waitingThreads.remove(Thread.currentThread());
              numOfWaitingThreards--;
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

      Thread.interrupted(); // Clear interrupted flag if the thread is interrupted during wait.
    }

    public long awaitNanos(long nanosTimeout) throws InterruptedException {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      if (Thread.interrupted()) { throw new InterruptedException(); }

      int numOfHolds = originalLock.getHoldCount();
      try {
        ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
        UnsafeUtil.monitorEnter(realCondition);
        boolean isLockInUnshared = isLockRealConditionInUnshared();
        try {
          fullRelease();
          waitingThreads.add(Thread.currentThread());
          numOfWaitingThreards++;

          addWaitOnUnshared();
          try {
            long startTime = getSystemNanos();
            TimeUnit.NANOSECONDS.timedWait(realCondition, nanosTimeout);
            long remainingTime = nanosTimeout - (getSystemNanos() - startTime);
            return remainingTime;
          } finally {
            waitOnUnshared.remove(Thread.currentThread());
            waitingThreads.remove(Thread.currentThread());
            numOfWaitingThreards--;
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
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }

      ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(realCondition);
      boolean isLockInUnshared = isLockRealConditionInUnshared();
      try {
        ManagerUtil.objectNotify(realCondition);
        if (hasWaitOnUnshared()) {
          realCondition.notify();
        }
        signal = SIGNALLED;
      } finally {
        UnsafeUtil.monitorExit(realCondition);
        if (!isLockInUnshared) {
          ManagerUtil.monitorExit(realCondition);
        }
      }

    }

    public void signalAll() {
      if (!originalLock.isHeldByCurrentThread()) { throw new IllegalMonitorStateException(); }
      ManagerUtil.monitorEnter(realCondition, LockLevel.WRITE);
      UnsafeUtil.monitorEnter(realCondition);
      boolean isLockInUnshared = isLockRealConditionInUnshared();
      try {
        ManagerUtil.objectNotifyAll(realCondition);
        if (hasWaitOnUnshared()) {
          realCondition.notifyAll();
        }
        signal = SIGNALLED;
      } finally {
        UnsafeUtil.monitorExit(realCondition);
        if (!isLockInUnshared) {
          ManagerUtil.monitorExit(realCondition);
        }
      }
    }

    int getWaitQueueLength(ReentrantLock lock) {
      if (originalLock != lock) throw new IllegalArgumentException("not owner");
      if (!ManagerUtil.isManaged(originalLock)) {
        return numOfWaitingThreards;
      } else {
        return ManagerUtil.waitLength(realCondition);
      }
    }

    Collection getWaitingThreads(ReentrantLock lock) {
      if (originalLock != lock) throw new IllegalArgumentException("not owner");
      Assert.assertFalse(ManagerUtil.isManaged(originalLock));
      return waitingThreads;
    }
  }
}
