/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util.concurrent.locks;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCObjectNotSharableException;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.NotClearable;
import com.tc.util.concurrent.locks.TCLock;
import com.tcclient.util.concurrent.locks.ConditionObject;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ReentrantLockTC extends ReentrantLock implements TCLock, NotClearable {
  public void lock() {
    ManagerUtil.monitorEnter(this, Manager.LOCK_TYPE_WRITE);
    super.lock();
  }

  public void lockInterruptibly() throws InterruptedException {
    if (Thread.interrupted()) { throw new InterruptedException(); }

    ManagerUtil.monitorEnterInterruptibly(this, Manager.LOCK_TYPE_WRITE);
    try {
      super.lockInterruptibly();
    } catch (InterruptedException e) {
      ManagerUtil.monitorExit(this, Manager.LOCK_TYPE_WRITE);
      throw e;
    }
  }

  public Condition newCondition() {
    return new ConditionObject(this);
  }

  public void unlock() {
    super.unlock();
    ManagerUtil.monitorExit(this, Manager.LOCK_TYPE_WRITE);
  }

  public int getHoldCount() {
    return super.getHoldCount();
  }

  protected Thread getOwner() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getOwner();
    }
  }

  protected Collection<Thread> getQueuedThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getQueuedThreads();
    }
  }

  protected Collection<Thread> getWaitingThreads(Condition condition) {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      if (condition == null) throw new NullPointerException();
      if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
      return ((ConditionObject) condition).getWaitingThreads(this);
    }
  }

  public int getWaitQueueLength(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitQueueLength(this);
  }

  public boolean hasWaiters(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitQueueLength(this) > 0;
  }

  public boolean isLocked() {
    if (ManagerUtil.isManaged(this)) {
      return isHeldByCurrentThread() || ManagerUtil.isLocked(this, Manager.LOCK_TYPE_WRITE);
    } else {
      return super.isLocked();
    }
  }
  
  private String getLockState() {
    return isLocked() ? (isHeldByCurrentThread() ? "[Locked by current thread]" : "[Locked by other thread]") : "[Unlocked]";
  }

  public String toString() {
    if (ManagerUtil.isManaged(this)) {
      String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
      return (new StringBuilder()).append(objectString).append(getLockState()).toString();
    } else {
      return super.toString();
    }
  }

  private boolean dsoTryLock() {
    if (ManagerUtil.isManaged(this)) {
      return ManagerUtil.tryMonitorEnter(this, Manager.LOCK_TYPE_WRITE);
    } else {
      return true;
    }
  }

  public boolean dsoTryLock(long timeout, TimeUnit unit) throws InterruptedException {
    if (ManagerUtil.isManaged(this)) {
      long timeoutInNanos = TimeUnit.NANOSECONDS.convert(timeout, unit);
      return ManagerUtil.tryMonitorEnter(this, Manager.LOCK_TYPE_WRITE, timeoutInNanos);
    } else {
      return true;
    }
  }

  public boolean tryLock() {
    boolean isLocked = dsoTryLock();
    if (isLocked || !ManagerUtil.isManaged(this)) {
      isLocked = super.tryLock();
    }
    return isLocked;
  }

  public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    boolean isLocked = dsoTryLock(timeout, unit);
    if (isLocked || !ManagerUtil.isManaged(this)) {
      isLocked = super.tryLock(timeout, unit);
    }
    return isLocked;
  }

  public int localHeldCount() {
    return getHoldCount();
  }

  public void validateInUnLockState() {
    if (super.isLocked()) { throw new TCObjectNotSharableException(
                                                           "You are attempting to share a ReentrantLock when it is in a locked state. Lock cannot be shared while locked."); }
  }
}
