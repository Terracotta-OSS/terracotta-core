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

  private static ToolkitLockTypeInternal getWriteLockType(ToolkitLockTypeInternal writeLockType) {
    return writeLockType == ToolkitLockTypeInternal.SYNCHRONOUS_WRITE ? ToolkitLockTypeInternal.SYNCHRONOUS_WRITE
        : ToolkitLockTypeInternal.WRITE;
  }

  UnnamedToolkitReadWriteLock(PlatformService platformService, String lockId, ToolkitLockTypeInternal writeLockType) {
    this(new UnnamedToolkitLock(platformService, lockId, getWriteLockType(writeLockType)),
         new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.READ));
  }

  UnnamedToolkitReadWriteLock(PlatformService platformService, long lockId, ToolkitLockTypeInternal writeLockType) {
    this(new UnnamedToolkitLock(platformService, lockId, getWriteLockType(writeLockType)),
         new UnnamedToolkitLock(platformService, lockId, ToolkitLockTypeInternal.READ));
  }

  private UnnamedToolkitReadWriteLock(ToolkitLock writeLock, ToolkitLock readLock) {
    if (writeLock.getLockType() != ToolkitLockType.WRITE) { throw new AssertionError("lockType "
                                                                                     + writeLock.getLockType()
                                                                                     + " not supported for "
                                                                                     + ToolkitLockType.WRITE + " lock"); }
    if (readLock.getLockType() != ToolkitLockType.READ) { throw new AssertionError("lockType " + readLock.getLockType()
                                                                                   + " not supported for "
                                                                                   + ToolkitLockType.READ + " lock"); }
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
