/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class UnnamedToolkitLock implements ToolkitLock {
  private final ToolkitLockDetail lockDetail;
  private final ConditionImpl   conditionImpl = new ConditionImpl();
  private final PlatformService   service;

  UnnamedToolkitLock(PlatformService platformService, String lockId, ToolkitLockTypeInternal lockType) {
    this(platformService, ToolkitLockDetail.newLockDetail(lockId, lockType));
  }

  UnnamedToolkitLock(PlatformService platformService, long lockId, ToolkitLockTypeInternal lockType) {
    this(platformService, ToolkitLockDetail.newLockDetail(lockId, lockType));
  }

  private UnnamedToolkitLock(PlatformService platformService, ToolkitLockDetail lockDetail) {
    this.lockDetail = lockDetail;
    this.service = platformService;
  }

  @Override
  public void lock() {
    ToolkitLockingApi.lock(lockDetail, service);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    ToolkitLockingApi.lockInterruptibly(lockDetail, service);
  }

  @Override
  public boolean tryLock() {
    return ToolkitLockingApi.tryLock(lockDetail, service);
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return ToolkitLockingApi.tryLock(lockDetail, time, unit, service);
  }

  @Override
  public void unlock() {
    ToolkitLockingApi.unlock(lockDetail, service);
  }

  @Override
  public boolean isHeldByCurrentThread() {
    return ToolkitLockingApi.isHeldByCurrentThread(lockDetail, service);
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
    ToolkitLockTypeInternal lockType = lockDetail.getToolkitInternalLockType();
    if (lockType == ToolkitLockTypeInternal.READ || lockType == ToolkitLockTypeInternal.CONCURRENT) {
      //
      throw new UnsupportedOperationException("Conditions not supported for this lock type - " + lockType);
    }
    return conditionImpl;
  }

  @Override
  public ToolkitLockType getLockType() {
    return lockDetail.getToolkitInternalLockType().getToolkitLockType();
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
            ToolkitLockingApi.lockIdWait(lockDetail, service);
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

      ToolkitLockingApi.lockIdWait(lockDetail, nanosTimeout, TimeUnit.NANOSECONDS, service);

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
      ToolkitLockingApi.lockIdNotify(lockDetail, service);
    }

    @Override
    public void signalAll() {
      ToolkitLockingApi.lockIdNotifyAll(lockDetail, service);
    }

  }

}