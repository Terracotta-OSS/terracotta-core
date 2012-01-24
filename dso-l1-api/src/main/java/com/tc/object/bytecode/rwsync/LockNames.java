/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

/**
 * Types and field names used for locking. These are in an interface so that both method adapters and class adapters can
 * share them.
 */
public class LockNames {
  // Type names
  public static final String RWLOCK_TYPE           = "java/util/concurrent/locks/ReentrantReadWriteLock";
  public static final String READLOCK_TYPE         = RWLOCK_TYPE + "$ReadLock";
  public static final String WRITELOCK_TYPE        = RWLOCK_TYPE + "$WriteLock";

  // Type descriptors
  public static final String RWLOCK_DESC           = "L" + RWLOCK_TYPE + ";";
  public static final String READLOCK_DESC         = "L" + RWLOCK_TYPE + "$ReadLock;";
  public static final String WRITELOCK_DESC        = "L" + RWLOCK_TYPE + "$WriteLock;";

  // Method names for ReadLock and WriteLock
  public static final String UNLOCK_METHOD_NAME    = "unlock";
  public static final String UNLOCK_METHOD_DESC    = "()V";
  public static final String LOCK_METHOD_NAME      = "lock";
  public static final String LOCK_METHOD_DESC      = "()V";

  // Field names
  public static final String RWLOCK_NAME           = "__tc_readWriteLock";
  public static final String READLOCK_NAME         = "__tc_readLock";
  public static final String WRITELOCK_NAME        = "__tc_writeLock";

  // Method names to obtain read and write lock
  public static final String READLOCK_METHOD_NAME  = "readLock";
  public static final String WRITELOCK_METHOD_NAME = "writeLock";
  public static final String READLOCK_METHOD_DESC  = "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;";
  public static final String WRITELOCK_METHOD_DESC = "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$WriteLock;";
}
