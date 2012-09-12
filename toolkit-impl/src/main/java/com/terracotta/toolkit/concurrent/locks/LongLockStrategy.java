/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.bytecode.ManagerUtil;

public class LongLockStrategy<K> implements LockStrategy<Long, K> {

  private final long highBits;

  public LongLockStrategy(String instanceQualifier) {
    highBits = ((long) instanceQualifier.hashCode()) << 32;
  }

  @Override
  public Long generateLockIdForKey(K key) {
    long lowBits = key.hashCode() & 0x00000000FFFFFFFFL;
    return highBits | lowBits;
  }

  @Override
  public void lock(Long lockId, ToolkitLockTypeInternal type) {
    ManagerUtil.beginLock(lockId, LockingUtils.translate(type));
  }

  @Override
  public void commitLock(Long lockID, ToolkitLockTypeInternal type) {
    ManagerUtil.commitLock(lockID, LockingUtils.translate(type));
  }

  @Override
  public boolean beginLock(Long lockID, ToolkitLockTypeInternal type, long nanos) throws InterruptedException {
    return ManagerUtil.tryBeginLock(lockID, LockingUtils.translate(type), nanos);
  }

}
