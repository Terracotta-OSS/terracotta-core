/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import com.tc.net.GroupID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.locks.LockLevel;

public final class RootsUtil {

  private RootsUtil() {
    // private
  }

  public static interface RootObjectCreator<T> {
    T create();
  }

  public static <T> T lookupOrCreateRootInGroup(GroupID gid, String name, RootObjectCreator<T> creator) {
    String lockId = generateLockId(name);
    ManagerUtil.beginLock(lockId, LockLevel.READ);
    try {
      Object root = ManagerUtil.lookupRoot(name, gid);
      if (root != null) { return (T) root; }
    } finally {
      ManagerUtil.commitLock(lockId, LockLevel.READ);
    }

    ManagerUtil.beginLock(lockId, LockLevel.WRITE);
    try {
      return (T) ManagerUtil.lookupOrCreateRoot(name, creator.create(), gid);
    } finally {
      ManagerUtil.commitLock(lockId, LockLevel.WRITE);
    }
  }

  private static String generateLockId(String name) {
    return "__TC_ROOT_" + name;
  }
}
