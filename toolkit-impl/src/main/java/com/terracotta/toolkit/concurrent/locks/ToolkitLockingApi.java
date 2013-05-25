/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.abortable.AbortedOperationException;
import com.tc.object.ObjectID;
import com.tc.platform.PlatformService;
import com.tc.util.Assert;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.TimeUnit;

public class ToolkitLockingApi {

  private static final String TOOLKIT_OBJECT_LOCK_PREFIX = "toolkit-object-lock-prefix";
  private static final String DELIMITER                  = "|";

  private ToolkitLockingApi() {
    // private
  }

  // used just for txn boundaries
  public static UnnamedToolkitLock createConcurrentTransactionLock(String name, PlatformService service) {
    return new UnnamedToolkitLock(service, name, ToolkitLockTypeInternal.CONCURRENT);
  }

  // locks created for name based toolkitObjectTypes
  public static UnnamedToolkitLock createUnnamedLocked(ToolkitObjectType toolkitObjectType, String lockName,
                                                       ToolkitLockTypeInternal lockType, PlatformService service) {
    return new UnnamedToolkitLock(service, generateStringLockId(toolkitObjectType, lockName), lockType);
  }

  // RWLsocks created for toolkitLockedObjects
  public static UnnamedToolkitReadWriteLock createUnnamedReadWriteLock(ToolkitObjectType type, ObjectID oid,
                                                                       PlatformService service,
                                                                       ToolkitLockTypeInternal writeLockType) {
    return new UnnamedToolkitReadWriteLock(service, generateStringLockId(type, String.valueOf(oid.toLong())),
                                           writeLockType);
  }

  // RWLsocks created for toolkitObject - used for ToolkitReadWriteLock
  public static UnnamedToolkitReadWriteLock createUnnamedReadWriteLock(ToolkitObjectType toolkitObjectType,
                                                                       String name, PlatformService service,
                                                                       ToolkitLockTypeInternal writeLockType) {
    return new UnnamedToolkitReadWriteLock(service, generateStringLockId(toolkitObjectType, name), writeLockType);
  }

  // used for ServerMap keys
  public static UnnamedToolkitReadWriteLock createUnnamedReadWriteLock(Object lockId, PlatformService service,
                                                                       ToolkitLockTypeInternal writeLockType) {
    assertLockIdType(lockId);
    if (lockId instanceof Long) { return new UnnamedToolkitReadWriteLock(service, (Long) lockId, writeLockType); }
    if (lockId instanceof String) { return new UnnamedToolkitReadWriteLock(service, (String) lockId, writeLockType); }
    return null;
  }

  public static void lock(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      service.beginLock(lockDetail.getLockId(), lockDetail.getLockLevel());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void unlock(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      service.commitLock(lockDetail.getLockId(), lockDetail.getLockLevel());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void lockInterruptibly(ToolkitLockDetail lockDetail, PlatformService service)
      throws InterruptedException {
    try {
      service.beginLockInterruptibly(lockDetail.getLockId(), lockDetail.getLockLevel());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static boolean tryLock(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      return service.tryBeginLock(lockDetail.getLockId(), lockDetail.getLockLevel());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static boolean tryLock(ToolkitLockDetail lockDetail, long time, TimeUnit unit, PlatformService service)
      throws InterruptedException {
    try {
      return service.tryBeginLock(lockDetail.getLockId(), lockDetail.getLockLevel(), time, unit);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static boolean isHeldByCurrentThread(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      return service.isHeldByCurrentThread(lockDetail.getLockId(), lockDetail.getLockLevel());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void lockIdWait(ToolkitLockDetail lockDetail, PlatformService service) throws InterruptedException {
    // 0 timeout means infinite wait
    try {
      service.lockIDWait(lockDetail.getLockId(), 0, TimeUnit.MILLISECONDS);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void lockIdWait(ToolkitLockDetail lockDetail, long time, TimeUnit unit, PlatformService service)
      throws InterruptedException {
    // 0 timeout means infinite wait
    try {
      service.lockIDWait(lockDetail.getLockId(), time, unit);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void lockIdNotify(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      service.lockIDNotify(lockDetail.getLockId());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  public static void lockIdNotifyAll(ToolkitLockDetail lockDetail, PlatformService service) {
    try {
      service.lockIDNotifyAll(lockDetail.getLockId());
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  private static void assertLockIdType(Object lockId) {
    boolean condition = (lockId instanceof String) || (lockId instanceof Long);
    Assert.assertTrue("lockId should be String OR Long but " + lockId.getClass().getName(), condition);
  }

  public static void lock(Object lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    assertLockIdType(lockId);
    if (lockId instanceof String) {
      lock((String) lockId, lockType, service);
    }
    if (lockId instanceof Long) {
      lock((Long) lockId, lockType, service);
    }
  }

  public static void unlock(Object lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    assertLockIdType(lockId);
    if (lockId instanceof String) {
      unlock((String) lockId, lockType, service);
    }
    if (lockId instanceof Long) {
      unlock((Long) lockId, lockType, service);
    }
  }
  
  private static void lock(Long lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    doBeginLock(lockId, lockType, service);
  }

  private static void unlock(Long lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    doCommitLock(lockId, lockType, service);
  }

  private static void lock(String lockName, ToolkitLockTypeInternal lockType, PlatformService service) {
    doBeginLock(lockName, lockType, service);
  }

  private static void unlock(String lockName, ToolkitLockTypeInternal lockType, PlatformService service) {
    doCommitLock(lockName, lockType, service);
  }

  public static void lock(ToolkitObjectType toolkitObjectType, String toolkitObjectName,
                          ToolkitLockTypeInternal lockType, PlatformService service) {
    doBeginLock(generateStringLockId(toolkitObjectType, toolkitObjectName), lockType, service);
  }

  public static void unlock(ToolkitObjectType toolkitObjectType, String toolkitObjectName,
                            ToolkitLockTypeInternal lockType, PlatformService service) {
    doCommitLock(generateStringLockId(toolkitObjectType, toolkitObjectName), lockType, service);
  }

  private static void doBeginLock(Object lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    try {
      service.beginLock(lockId, LockingUtils.translate(lockType));
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  private static void doCommitLock(Object lockId, ToolkitLockTypeInternal lockType, PlatformService service) {
    try {
      service.commitLock(lockId, LockingUtils.translate(lockType));
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  private static String generateStringLockId(ToolkitObjectType toolkitObjectType, String toolkitObjectName) {
    return TOOLKIT_OBJECT_LOCK_PREFIX + DELIMITER + toolkitObjectType.name() + DELIMITER + toolkitObjectName;
  }

}
