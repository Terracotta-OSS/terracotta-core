/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util.concurrent.locks;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCObjectNotSharableException;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.util.DebugUtil;
import com.tc.util.concurrent.locks.TCLock;
import com.tcclient.util.concurrent.locks.ConditionObject;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLockTC extends ReentrantReadWriteLock {
  private ReentrantReadWriteLock.ReadLock  readerLock;
  private ReentrantReadWriteLock.WriteLock writerLock;
  private ReentrantReadWriteLock.Sync      sync;

  private static class DsoLock {
    private final ReentrantReadWriteLockTC lock;
    private final int                      lockLevel;

    public DsoLock(ReentrantReadWriteLockTC lock, int lockLevel) {
      this.lock = lock;
      this.lockLevel = lockLevel;
    }

    public void lock() {
      if (ManagerUtil.isManaged(lock)) {
        ManagerUtil.monitorEnter(lock, lockLevel);
      }
    }

    public boolean tryLock() {
      if (ManagerUtil.isManaged(lock)) {
        return ManagerUtil.tryMonitorEnter(lock, 0, lockLevel);
      } else {
        return true;
      }
    }

    public boolean tryLock(long timeout, TimeUnit unit) {
      if (ManagerUtil.isManaged(lock)) {
        long timeoutInNanos = TimeUnit.NANOSECONDS.convert(timeout, unit);
        return ManagerUtil.tryMonitorEnter(lock, timeoutInNanos, lockLevel);
      } else {
        return true;
      }
    }

    public void unlock() {
      if (ManagerUtil.isManaged(lock)) {
        ManagerUtil.monitorExit(lock);
      }
    }

    public ReentrantReadWriteLockTC getDsoLock() {
      return lock;
    }

    public String getLockState(int lockLevel) {
      return (ManagerUtil.isLocked(lock, lockLevel) ? (ManagerUtil.isHeldByCurrentThread(lock, lockLevel) ? "[Locally locked]"
          : "[Remotelly locked]")
          : "[Unlocked]");
    }

  }

  public static class ReadLock extends ReentrantReadWriteLock.ReadLock implements Manageable {
    private volatile transient TCObject $__tc_MANAGED;
    private transient DsoLock           dsoLock;
    private Sync                        sync;

    protected ReadLock(ReentrantReadWriteLockTC lock, Sync sync) {
      super(lock);
      dsoLock = new DsoLock(lock, LockLevel.READ);
    }

    void init() {
      sync = dsoLock.getDsoLock().getSync();
    }

    public void lock() {
      dsoLock.lock();
      super.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
      if (Thread.interrupted()) { throw new InterruptedException(); }

      try {
        dsoLock.lock();
        super.lockInterruptibly();
      } finally {
        if (Thread.interrupted()) {
          unlock();
          throw new InterruptedException();
        }
      }
    }

    public boolean tryLock() {
      boolean isLocked = dsoLock.tryLock();
      if (isLocked || !ManagerUtil.isManaged(dsoLock.getDsoLock())) {
        isLocked = super.tryLock();
      }
      return isLocked;
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      boolean isLocked = dsoLock.tryLock(timeout, unit);
      if (isLocked || !ManagerUtil.isManaged(dsoLock.getDsoLock())) {
        isLocked = super.tryLock(timeout, unit);
      }
      return isLocked;
    }

    public void unlock() {
      super.unlock();
      dsoLock.unlock();
    }

    public String toString() {
      if (ManagerUtil.isManaged(this)) {
        String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
        return (new StringBuilder()).append(objectString).append(dsoLock.getLockState(LockLevel.READ)).toString();
      } else {
        return super.toString();
      }
    }

    public void __tc_managed(TCObject tcObject) {
      $__tc_MANAGED = tcObject;
    }

    public TCObject __tc_managed() {
      return $__tc_MANAGED;
    }

    public boolean __tc_isManaged() {
      // TCObject tcManaged = $__tc_MANAGED;
      // return (tcManaged != null && (tcManaged instanceof TCObjectPhysical || tcManaged instanceof TCObjectLogical));
      return $__tc_MANAGED != null;
    }
  }

  public static class WriteLock extends ReentrantReadWriteLock.WriteLock implements TCLock, Manageable {
    private volatile transient TCObject $__tc_MANAGED;
    private transient DsoLock           dsoLock;
    private Sync                        sync;

    protected WriteLock(ReentrantReadWriteLockTC lock, Sync sync) {
      super(lock);
      this.dsoLock = new DsoLock(lock, LockLevel.WRITE);
    }

    void init() {
      sync = dsoLock.getDsoLock().getSync();
    }

    public void lock() {
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " in ReentrantReadWriteLock.lock() -- dsoLock: "
                           + ((dsoLock == null) ? "null" : "not null") + ", sync: " + sync);
      }
      dsoLock.lock();
      super.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
      if (Thread.interrupted()) { throw new InterruptedException(); }

      try {
        dsoLock.lock();
        super.lockInterruptibly();
      } finally {
        if (Thread.interrupted()) {
          unlock();
          throw new InterruptedException();
        }
      }
    }

    public boolean tryLock() {
      boolean isLocked = dsoLock.tryLock();
      if (isLocked || !ManagerUtil.isManaged(dsoLock.getDsoLock())) {
        isLocked = super.tryLock();
      }
      return isLocked;
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      boolean isLocked = dsoLock.tryLock(timeout, unit);
      if (isLocked || !ManagerUtil.isManaged(dsoLock.getDsoLock())) {
        isLocked = super.tryLock(timeout, unit);
      }
      return isLocked;
    }

    public void unlock() {
      super.unlock();
      dsoLock.unlock();
    }

    public Condition newCondition() {
      return new ConditionObject(this);
    }

    public Object getTCLockingObject() {
      return dsoLock.getDsoLock();
    }

    public boolean isHeldByCurrentThread() {
      return dsoLock.getDsoLock().isWriteLockedByCurrentThread();
    }

    public int localHeldCount() {
      return dsoLock.getDsoLock().getWriteHoldCount();
    }

    public String toString() {
      if (ManagerUtil.isManaged(this)) {
        String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
        return (new StringBuilder()).append(objectString).append(dsoLock.getLockState(LockLevel.WRITE)).toString();
      } else {
        return super.toString();
      }
    }

    public void __tc_managed(TCObject tcObject) {
      $__tc_MANAGED = tcObject;
    }

    public TCObject __tc_managed() {
      return $__tc_MANAGED;
    }

    public boolean __tc_isManaged() {
      // TCObject tcManaged = $__tc_MANAGED;
      // return (tcManaged != null && (tcManaged instanceof TCObjectPhysical || tcManaged instanceof TCObjectLogical));
      return $__tc_MANAGED != null;
    }
  }

  protected Thread getOwner() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getOwner();
    }
  }

  protected Collection<Thread> getQueuedReaderThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getQueuedReaderThreads();
    }
  }

  protected Collection<Thread> getQueuedThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getQueuedThreads();
    }
  }

  protected Collection<Thread> getQueuedWriterThreads() {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      return super.getQueuedWriterThreads();
    }
  }

  // TODO: need to review
  public int getReadLockCount() {
    if (ManagerUtil.isManaged(this)) {
      return ManagerUtil.localHeldCount(this, LockLevel.READ);
    } else {
      return super.getReadLockCount();
    }
  }

  protected Collection<Thread> getWaitingThreads(Condition condition) {
    if (ManagerUtil.isManaged(this)) {
      throw new TCNotSupportedMethodException();
    } else {
      if (condition == null) throw new NullPointerException();
      if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
      return ((ConditionObject) condition).getWaitingThreads(writeLock());
    }
  }

  public int getWaitQueueLength(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitQueueLength(writeLock());
  }

  public int getWriteHoldCount() {
    return super.getWriteHoldCount();
  }

  public boolean hasWaiters(Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof ConditionObject)) throw new IllegalArgumentException("not owner");
    return ((ConditionObject) condition).getWaitQueueLength(writeLock()) > 0;
  }

  public boolean isWriteLocked() {
    if (ManagerUtil.isManaged(this)) {
      return ManagerUtil.isLocked(this, LockLevel.WRITE);
    } else {
      return super.isWriteLocked();
    }
  }

  public boolean isWriteLockedByCurrentThread() {
    return super.isWriteLockedByCurrentThread();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    readerLock = new ReadLock(this, sync);
    writerLock = new WriteLock(this, sync);
    if (DebugUtil.DEBUG) {
      System.err.println("DEBUG: in readObject() -- sync: " + sync);
    }
  }

  public void validateInUnLockState() {
    boolean isLocked = super.getReadLockCount() != 0 || super.isWriteLocked();
    if (isLocked) { throw new TCObjectNotSharableException(
                                                           "You attempt to share a ReentrantReadWriteLock when it is in a lock state. Lock cannot be in a locked state when shared."); }
  }

  public Sync getSync() {
    return sync;
  }

  // TODO: need to review
  public String toString() {
    if (DebugUtil.DEBUG) {
      System.err.println("DEBUG: in toString() -- sync: " + sync);
    }
    if (ManagerUtil.isManaged(this)) {
      String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
      return objectString + "[Write locks = " + writeLock() + ", Read locks = " + readLock() + "]";
    } else {
      return super.toString();
    }
  }
}
