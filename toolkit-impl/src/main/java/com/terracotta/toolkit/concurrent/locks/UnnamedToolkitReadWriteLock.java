/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;

public final class UnnamedToolkitReadWriteLock implements ToolkitReadWriteLock {
  private final ToolkitLock writeLock;
  private final ToolkitLock readLock;

  UnnamedToolkitReadWriteLock(PlatformService platformService, String lockId) {
    this(new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.WRITE),
         new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.READ));
  }

  UnnamedToolkitReadWriteLock(PlatformService platformService, long lockId) {
    this(new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.WRITE),
         new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.READ));
  }

  private UnnamedToolkitReadWriteLock(ToolkitLock writeLock, ToolkitLock readLock) {
    if (writeLock.getLockType() != ToolkitLockType.WRITE) { throw new AssertionError(
                                                                                     "Write lock instance should have WRITE lock type"); }
    if (readLock.getLockType() != ToolkitLockType.READ) { throw new AssertionError(
                                                                                   "Read lock instance should have READ lock type"); }
    this.writeLock = writeLock;
    this.readLock = readLock;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public ToolkitLock readLock() {
    return this.readLock;
  }

  @Override
  public ToolkitLock writeLock() {
    return this.writeLock;
  }
}
