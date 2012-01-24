/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

/**
 * Defines a lock.  Locks may be either auto locks or named locks.  Both types of locks have four locking
 * levels: WRITE, READ, CONCURRENT, and SYNCHRONOUS_WRITE.  Auto locks may also be auto-synchronized.  The 
 * locking level and auto-synchronized flag are defined in the ConfigLockLevel.  
 */
public interface LockDefinition {

  /**
   * Name to use with autolocks: "tc:autolock"
   */
  public static final String TC_AUTOLOCK_NAME = "tc:autolock";

  /**
   * @param lockName Lock name
   */
  public void setLockName(String lockName);

  /**
   * @return Lock name, {@link #TC_AUTOLOCK_NAME} for auto locks
   */
  public String getLockName();

  /**
   * @param lt Lock level
   */
  public void setLockLevel(ConfigLockLevel lt);

  /**
   * @return Lock level
   */
  public ConfigLockLevel getLockLevel();

  /**
   * @return Lock level as code defining level
   * @see ConfigLockLevel
   */
  public int getLockLevelAsInt();
  
  /**
   * @return Configuration text of the lock definition
   */
  public String getLockContextInfo();

  /**
   * @return True if auto lock, false if named lock
   */
  public boolean isAutolock();

  /**
   * Commit this definition, after which the definition cannot be changed.
   */
  public void commit();

}
