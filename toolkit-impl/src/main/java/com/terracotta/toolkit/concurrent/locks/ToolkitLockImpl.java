/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class ToolkitLockImpl implements ToolkitLock {
  private final String      lockName;
  private final ToolkitLock delegate;

  public ToolkitLockImpl(PlatformService platformService, String name, ToolkitLockTypeInternal lockType) {
    this.lockName = name;
    this.delegate = ToolkitLockingApi.createUnnamedLocked(ToolkitObjectType.LOCK, lockName, lockType, platformService);
  }

  @Override
  public String getName() {
    return lockName;
  }

  @Override
  public void lock() {
    delegate.lock();
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    delegate.lockInterruptibly();
  }

  @Override
  public boolean tryLock() {
    return delegate.tryLock();
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return delegate.tryLock(time, unit);
  }

  @Override
  public void unlock() {
    delegate.unlock();
  }

  @Override
  public Condition newCondition() throws UnsupportedOperationException {
    return delegate.newCondition();
  }

  @Override
  public Condition getCondition() {
    return delegate.getCondition();
  }

  @Override
  public ToolkitLockType getLockType() {
    return delegate.getLockType();
  }

  @Override
  public boolean isHeldByCurrentThread() {
    return delegate.isHeldByCurrentThread();
  }

}