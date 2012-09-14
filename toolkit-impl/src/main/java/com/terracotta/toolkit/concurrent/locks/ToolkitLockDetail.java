/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.locks.LockLevel;

// package protected class
class ToolkitLockDetail {

  private final Object                  lockId;
  private final ToolkitLockTypeInternal lockType;

  public static ToolkitLockDetail newLockDetail(String stringLockId, ToolkitLockTypeInternal lockType) {
    return new ToolkitLockDetail(stringLockId, lockType);
  }

  public static ToolkitLockDetail newLockDetail(long longLockId, ToolkitLockTypeInternal lockType) {
    return new ToolkitLockDetail(longLockId, lockType);
  }

  private ToolkitLockDetail(Object lockId, ToolkitLockTypeInternal lockType) {
    this.lockId = lockId;
    this.lockType = lockType;
  }

  public Object getLockId() {
    return lockId;
  }

  public ToolkitLockTypeInternal getToolkitInternalLockType() {
    return lockType;
  }

  public LockLevel getLockLevel() {
    return LockingUtils.translate(lockType);
  }

}
