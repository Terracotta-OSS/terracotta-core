/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.locks.LockLevel;

public class LockingUtils {

  public static LockLevel translate(final ToolkitLockTypeInternal lockType) {
    switch (lockType) {
      case WRITE:
        return LockLevel.WRITE;
      case READ:
        return LockLevel.READ;
      case SYNCHRONOUS_WRITE:
        return LockLevel.SYNCHRONOUS_WRITE;
      case CONCURRENT:
        return LockLevel.CONCURRENT;

    }

    // don't do this as the "default" in the switch block so the compiler can catch errors
    throw new AssertionError("unknown lock type: " + lockType);
  }

  public static ToolkitLockTypeInternal translate(final LockLevel lockLevel) {
    switch (lockLevel) {
      case WRITE:
        return ToolkitLockTypeInternal.WRITE;
      case READ:
        return ToolkitLockTypeInternal.READ;
      case SYNCHRONOUS_WRITE:
        return ToolkitLockTypeInternal.SYNCHRONOUS_WRITE;
      case CONCURRENT:
        return ToolkitLockTypeInternal.CONCURRENT;
    }

    // don't do this as the "default" in the switch block so the compiler can catch errors
    throw new AssertionError("unknown lock level: " + lockLevel);
  }
}
