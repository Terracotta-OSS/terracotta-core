/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.NVPair;
import com.tc.search.SearchQueryResults;

import java.util.List;
import java.util.Set;

public class ManagerUtilInternal {

  private static final ManagerInternal NULL_MANAGER_INTERNAL = new NullManagerInternal();

  private ManagerUtilInternal() {
    //
  }

  public static ManagerInternal getInternalManager() {
    Manager manager = ManagerUtil.getManager();
    if (manager instanceof NullManager) { return NULL_MANAGER_INTERNAL; }
    return (ManagerInternal) manager;
  }

  public static MetaDataDescriptor createMetaDataDescriptor(String category) {
    return getInternalManager().createMetaDataDescriptor(category);
  }

  public static SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys,
                                                boolean includeValues, Set<String> attributeSet,
                                                List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults,
                                                int batchSize) {
    return getInternalManager().executeQuery(cachename, queryStack, includeKeys, includeValues, attributeSet,
                                             sortAttributes, aggregators, maxResults, batchSize);
  }

  public static NVPair createNVPair(String name, Object value) {
    return getInternalManager().createNVPair(name, value);
  }

  /**
   * Begin lock
   * 
   * @param long lockID Lock identifier
   * @param type Lock type
   */
  public static void beginLock(final long lockID, final int type) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to begin lock
   * 
   * @param long lockID Lock identifier
   * @param type Lock type
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final long lockID, final int type) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type));
  }

  /**
   * Try to begin lock within a specific timespan
   * 
   * @param lockID Lock identifier
   * @param type Lock type
   * @param timeoutInNanos Timeout in nanoseconds
   * @return True if lock was successful
   */
  public static boolean tryBeginLock(final long lockID, final int type, final long timeoutInNanos)
      throws InterruptedException {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    return mgr.tryLock(lock, LockLevel.fromInt(type), timeoutInNanos / 1000000);
  }

  /**
   * Commit lock
   * 
   * @param long lockID Lock name
   */
  public static void commitLock(final long lockID, final int type) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  public static void pinLock(final long lockID) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.pinLock(lock);
  }

  public static void unpinLock(final long lockID) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockID);
    mgr.unpinLock(lock);
  }

  /**
   * Check whether this lock is held by the current thread
   * 
   * @param lockId The lock ID
   * @param lockLevel The lock level
   * @return True if held by current thread
   */
  public static boolean isLockHeldByCurrentThread(final long lockId, final int lockLevel) {
    ManagerInternal mgr = getInternalManager();
    LockID lock = mgr.generateLockIdentifier(lockId);
    return mgr.isLockedByCurrentThread(lock, LockLevel.fromInt(lockLevel));
  }

  public static void verifyCapability(String capability) {
    getInternalManager().verifyCapability(capability);
  }
}
