/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots.impl;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;

public final class RootsUtil {

  private RootsUtil() {
    // private
  }

  public static interface RootObjectCreator<T> {
    T create();
  }

  public static <T> T lookupOrCreateRootInGroup(PlatformService platformService, GroupID gid, String name,
                                                RootObjectCreator<T> creator) {
    ToolkitLockingApi.lock(name, ToolkitLockTypeInternal.READ, platformService);
    try {
      Object root = platformService.lookupRoot(name, gid);
      if (root != null) { return (T) root; }
    } finally {
      ToolkitLockingApi.unlock(name, ToolkitLockTypeInternal.READ, platformService);
    }

    ToolkitLockingApi.lock(name, ToolkitLockTypeInternal.WRITE, platformService);
    try {
      return (T) platformService.lookupOrCreateRoot(name, creator.create(), gid);
    } finally {
      ToolkitLockingApi.unlock(name, ToolkitLockTypeInternal.WRITE, platformService);
    }
  }
}
