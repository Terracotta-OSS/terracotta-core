/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util.concurrent.locks;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCObjectNotSharableException;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.locks.LockLevel;
import com.tc.util.concurrent.locks.TCLock;
import com.tcclient.util.concurrent.locks.ConditionObject;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLockTC extends ReentrantReadWriteLock implements NotClearable {
  private ReentrantReadWriteLock.ReadLock  readerLock;
  private ReentrantReadWriteLock.WriteLock writerLock;
  private Sync                             sync;

  public ReentrantReadWriteLock.WriteLock writeLock() {
    return writerLock;
  }

  public ReentrantReadWriteLock.ReadLock readLock() {
    return readerLock;
  }

  private static class DsoLock implements NotClearable {
    private final Object lock;
    private final int    lockLevel;

    public DsoLock(Object lock, int lockLevel) {
      this.lock = lock;
      this.lockLevel = lockLevel;
    }

    public void lock() {
      if (ManagerUtil.isManaged(lock)) {
        ManagerUtil.monitorEnter(lock, lockLevel);
      }
    }

    public void lockInterruptibly() throws InterruptedException {
      if (ManagerUtil.isManaged(lock)) {
        ManagerUtil.monitorEnterInterruptibly(lock, lockLevel);
      }
    }

    public boolean tryLock() {
      if (ManagerUtil.isManaged(lock)) {
        return ManagerUtil.tryMonitorEnter(lock, lockLevel);
      } else {
        return true;
      }
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      if (ManagerUtil.isManaged(lock)) {
        long timeoutInNanos = TimeUnit.NANOSECONDS.convert(timeout, unit);
        boolean rv = ManagerUtil.tryMonitorEnter(lock, lockLevel, timeoutInNanos);
        return rv;
      } else {
        return true;
      }
    }

    public void unlock() {
      if (ManagerUtil.isManaged(lock)) {
        ManagerUtil.monitorExit(lock, lockLevel);
      }
    }

    public Object getDsoLock() {
      return lock;
    }

    public String getLockState(int level) {
      return (ManagerUtil.isLocked(lock, level) ? (ManagerUtil.isHeldByCurrentThread(lock, level) ? "[Locally locked]"
          : "[Remotelly locked]") : "[Unlocked]");
    }

  }

  public static class ReadLock extends ReentrantReadWriteLock.ReadLock implements Manageable, NotClearable {
    private volatile transient TCObject $__tc_MANAGED;
    private transient DsoLock           dsoLock;
    private Sync                        sync;

    protected ReadLock(ReentrantReadWriteLockTC lock, Sync sync) {
      super(lock);
      dsoLock = new DsoLock(sync, LockLevel.READ.toInt());
    }

    public void lock() {
      dsoLock.lock();
      super.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
      if (Thread.interrupted()) { throw new InterruptedException(); }

      dsoLock.lockInterruptibly();
      try {
        super.lockInterruptibly();
      } catch (InterruptedException e) {
        dsoLock.unlock();
        throw e;
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

    public void validateInUnLockState() {
      boolean isLocked = sync.getReadLockCount() != 0;
      if (isLocked) { throw new TCObjectNotSharableException(
                                                             "You are attempting to share a ReentrantReadWriteLock.ReadLock when it is in a locked state. Lock cannot be shared while locked."); }
    }

    public String toString() {
      if (ManagerUtil.isManaged(this)) {
        String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
        return (new StringBuilder()).append(objectString).append(dsoLock.getLockState(LockLevel.READ.toInt())).toString();
      } else {
        return super.toString();
      }
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
      s.defaultReadObject();
      this.dsoLock = new DsoLock(sync, LockLevel.READ.toInt());
    }

    public void __tc_managed(TCObject tcObject) {
      $__tc_MANAGED = tcObject;
    }

    public TCObject __tc_managed() {
      return $__tc_MANAGED;
    }

    public boolean __tc_isManaged() {
      return $__tc_MANAGED != null;
    }
  }

  public static class WriteLock extends ReentrantReadWriteLock.WriteLock implements TCLock, Manageable, NotClearable {
    private volatile transient TCObject $__tc_MANAGED;
    private transient DsoLock           dsoLock;
    private Sync                        sync;

    protected WriteLock(ReentrantReadWriteLockTC lock, Sync sync) {
      super(lock);
      this.dsoLock = new DsoLock(sync, LockLevel.WRITE.toInt());
    }

    public void lock() {
      dsoLock.lock();
      super.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
      if (Thread.interrupted()) { throw new InterruptedException(); }

      dsoLock.lockInterruptibly();
      try {
        super.lockInterruptibly();
      } catch (InterruptedException e) {
        dsoLock.unlock();
        throw e;
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

    public boolean isHeldByCurrentThread() {
      return sync.isHeldExclusively();
    }

    public int localHeldCount() {
      return sync.getWriteHoldCount();
    }

    public void validateInUnLockState() {
      boolean isLocked = sync.isWriteLocked();
      if (isLocked) { throw new TCObjectNotSharableException(
                                                             "You are attempting to share a ReentrantReadWriteLock.WriteLock when it is in a locked state. Lock cannot be shared while locked."); }
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
      s.defaultReadObject();
      this.dsoLock = new DsoLock(sync, LockLevel.WRITE.toInt());
    }

    public String toString() {
      if (ManagerUtil.isManaged(this)) {
        String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
        return (new StringBuilder()).append(objectString).append(dsoLock.getLockState(LockLevel.WRITE.toInt())).toString();
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
      return ManagerUtil.localHeldCount(sync, LockLevel.READ.toInt());
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
      return ManagerUtil.isLocked(sync, LockLevel.WRITE.toInt());
    } else {
      return super.isWriteLocked();
    }
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    readerLock = new ReadLock(this, sync);
    writerLock = new WriteLock(this, sync);
  }

  public void validateInUnLockState() {
    boolean isLocked = super.getReadLockCount() != 0 || super.isWriteLocked();
    if (isLocked) { throw new TCObjectNotSharableException(
                                                           "You are attempting to share a ReentrantReadWriteLock when it is in a locked state. Lock cannot be shared while locked."); }
  }

  public Sync getSync() {
    return sync;
  }

  // TODO: need to review
  public String toString() {
    if (ManagerUtil.isManaged(this)) {
      String objectString = getClass().getName() + "@" + Integer.toHexString(hashCode());
      return objectString + "[Write locks = " + writeLock() + ", Read locks = " + readLock() + "]";
    } else {
      return super.toString();
    }
  }
}
