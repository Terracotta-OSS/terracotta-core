/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

public interface LockStrategy<L, K> {

  L generateLockIdForKey(K key);

  void lock(L lockId, ToolkitLockTypeInternal type);

  void commitLock(L lockID, ToolkitLockTypeInternal type);

  boolean beginLock(L lockID, ToolkitLockTypeInternal type, long nanos) throws InterruptedException;

}