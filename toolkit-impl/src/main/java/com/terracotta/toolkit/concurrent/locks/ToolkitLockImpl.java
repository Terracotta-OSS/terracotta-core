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