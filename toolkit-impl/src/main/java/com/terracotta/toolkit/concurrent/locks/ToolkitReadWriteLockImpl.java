/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;

public class ToolkitReadWriteLockImpl implements ToolkitReadWriteLock {
  private final String                      name;
  private final UnnamedToolkitReadWriteLock delegate;

  public ToolkitReadWriteLockImpl(PlatformService platformService, String name) {
    this.name = name;
    this.delegate = ToolkitLockingApi.createUnnamedReadWriteLock(ToolkitObjectType.READ_WRITE_LOCK, name,
                                                                 platformService, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ToolkitLock readLock() {
    return delegate.readLock();
  }

  @Override
  public ToolkitLock writeLock() {
    return delegate.writeLock();
  }
}
