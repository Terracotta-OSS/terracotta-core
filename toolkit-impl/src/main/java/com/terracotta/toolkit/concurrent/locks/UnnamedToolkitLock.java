/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.locks.LockLevel;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class UnnamedToolkitLock implements ToolkitLock {
  private final Object        lockID;
  private final LockLevel     level;
  private final ConditionImpl conditionImpl = new ConditionImpl();

  public UnnamedToolkitLock(String lockId, ToolkitLockTypeInternal lockType) {
    this(lockId, LockingUtils.translate(lockType));
  }

  public UnnamedToolkitLock(long lockId, ToolkitLockTypeInternal lockType) {
    this(lockId, LockingUtils.translate(lockType));
  }

  private UnnamedToolkitLock(Object lockId, LockLevel level) {
    this.lockID = lockId;
    this.level = level;
  }

  protected Object getLockId() {
    return lockID;
  }

  @Override
  public void lock() {
    ManagerUtil.monitorEnter(lockID, level);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    ManagerUtil.monitorEnterInterruptibly(lockID, level);
  }

  @Override
  public boolean tryLock() {
    return ManagerUtil.tryMonitorEnter(lockID, level);
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return ManagerUtil.tryMonitorEnter(lockID, level, unit.toNanos(time));
  }

  @Override
  public void unlock() {
    ManagerUtil.monitorExit(lockID, level);
  }

  @Override
  public boolean isHeldByCurrentThread() {
    return ManagerUtil.isHeldByCurrentThread(lockID, level);
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException(
                                            "Use the getCondition method to get the condition associated with this lock.");
  }

  @Override
  public Condition getCondition() {
    ToolkitLockTypeInternal lockType = getLockTypeInternal();
    if (lockType == ToolkitLockTypeInternal.READ || lockType == ToolkitLockTypeInternal.CONCURRENT) {
      //
      throw new UnsupportedOperationException("Conditions not supported for this lock type - " + lockType);
    }
    return conditionImpl;
  }

  private ToolkitLockTypeInternal getLockTypeInternal() {
    return LockingUtils.translate(level);
  }

  @Override
  public ToolkitLockType getLockType() {
    return getLockTypeInternal().getToolkitLockType();
  }

  private final class ConditionImpl implements Condition {

    @Override
    public void await() throws InterruptedException {
      awaitNanos(0);
    }

    @Override
    public void awaitUninterruptibly() {
      boolean interrupted = false;
      try {
        while (true) {
          try {
            ManagerUtil.lockIDWait(lockID, 0);
          } catch (InterruptedException e) {
            interrupted = true;
            continue;
          }
          break;
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        } else {
          return;
        }
      }
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {

      long finalTime = System.nanoTime() + nanosTimeout;

      ManagerUtil.lockIDWait(lockID, TimeUnit.NANOSECONDS.toMillis(nanosTimeout));

      return finalTime - System.nanoTime();
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
      return awaitNanos(unit.toNanos(time)) > 0;
    }

    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
      long timeout = deadline.getTime() - System.currentTimeMillis();
      return await(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void signal() {
      ManagerUtil.lockIDNotify(lockID);
    }

    @Override
    public void signalAll() {
      ManagerUtil.lockIDNotifyAll(lockID);
    }

  }

}