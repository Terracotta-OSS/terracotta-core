/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent.locks;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.Stack;
import com.tc.util.UnsafeUtil;
import com.tcclient.util.concurrent.locks.ConditionObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

public class ReentrantLock implements TCLock, java.io.Serializable {
  private boolean  isFair;

  /** Current owner thread */
  transient Thread owner            = null;
  transient int    numOfHolds       = 0;
  transient List<Thread> waitingQueue = new ArrayList<Thread>();
  transient int    state            = 0;
  transient int    numQueued        = 0;
  transient Stack  lockInUnShared   = new Stack();
  transient List   tryLockWaitQueue = new LinkedList();

  transient Object lock             = new Object();

  public ReentrantLock() {
    this.isFair = false;

    initialize();
  }

  public ReentrantLock(boolean fair) {
    this.isFair = fair;

    initialize();
  }

  private void initialize() {
    this.owner = null;
    this.numOfHolds = 0;
    this.waitingQueue = new ArrayList<Thread>();
    this.state = 0;
    this.numQueued = 0;
    this.lock = new Object();
    this.lockInUnShared = new Stack();
    this.tryLockWaitQueue = new LinkedList();
  }

  public void lock() {
    Thread currentThread = Thread.currentThread();
    boolean isInterrupted = false;
    synchronized (lock) {
      while (owner != null && owner != currentThread && lockInUnShared.contains(Boolean.TRUE)) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }

      waitingQueue.add(currentThread);
      numQueued++;
    }

    ManagerUtil.monitorEnter(this, LockLevel.WRITE);
    UnsafeUtil.monitorEnter(this);

    synchronized (lock) {
      innerSetLockState();
      waitingQueue.remove(currentThread);
      numQueued--;
    }
    if (isInterrupted) {
      currentThread.interrupt();
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
        canLock = ManagerUtil.tryMonitorEnter(this, 0, LockLevel.WRITE);
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
  
  private void addCurrentThreadToQueue() {
    waitingQueue.add(Thread.currentThread());
    numQueued++;
  }
  
  private void removeCurrentThreadFromQueue() {
    waitingQueue.remove(Thread.currentThread());
    numQueued--;
  }

  public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    Thread currentThread = Thread.currentThread();

    long totalTimeoutInNanos = unit.toNanos(timeout);

    while (totalTimeoutInNanos > 0 && !tryLock()) {
      synchronized (lock) {
        addCurrentThreadToQueue();

        if (owner != null && owner != currentThread && lockInUnShared.contains(Boolean.TRUE)) {
          try {
            totalTimeoutInNanos = waitForLocalLock(lock, totalTimeoutInNanos);
          } finally {
            removeCurrentThreadFromQueue();
          }
          continue;
        }
      }

      try {
        boolean isLocked = ManagerUtil.tryMonitorEnter(this, totalTimeoutInNanos, LockLevel.WRITE);
        if (isLocked) {
          UnsafeUtil.monitorEnter(this);
          synchronized (lock) {
            innerSetLockState();
          }
          return true;
        } else {
          synchronized(lock) {
            if (ManagerUtil.isManaged(this)) {
              return false;
            } else {
              totalTimeoutInNanos = waitForLocalLock(lock, totalTimeoutInNanos);
              continue;
            }
          }
        }
      } finally {
        synchronized (lock) {
          removeCurrentThreadFromQueue();
        }
      }
    }

    return (totalTimeoutInNanos > 0);
  }

  private long waitForLocalLock(Object waitObject, long totalTimeoutInNanos) throws InterruptedException {
    long startTime = System.nanoTime();
    synchronized (waitObject) {
      TimeUnit.NANOSECONDS.timedWait(waitObject, totalTimeoutInNanos);
    }
    long endTime = System.nanoTime();
    totalTimeoutInNanos -= (endTime - startTime);
    return totalTimeoutInNanos;
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
      if (!needDSOUnlock) {
        lock.notifyAll();
      }
    }
    if (needDSOUnlock) {
      ManagerUtil.monitorExit(this);
    }
  }

  public Condition newCondition() {
    return new ConditionObject(this);
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
      return isHeldByCurrentThread() || ManagerUtil.isLocked(this, LockLevel.WRITE);
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

  protected Collection<Thread> getQueuedThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      List<Thread> waitingThreads = null;
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

  protected Collection<Thread> getWaitingThreads(Condition condition) {
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
    if (!ManagerUtil.isManaged(this)
        || !ManagerUtil.isHeldByCurrentThread(this, LockLevel.WRITE)) {
      this.lockInUnShared.push(Boolean.TRUE);
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
    initialize();
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();
    s.writeBoolean(isFair);
  }

  public Object getTCLockingObject() {
    return this;
  }

  public int localHeldCount() {
    synchronized(lock) {
      return getHoldCount();
    }
  }

}
