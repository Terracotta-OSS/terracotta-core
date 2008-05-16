/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.config.lock.LockContextInfo;

public class LockRequest {
  private final LockID   lockID;
  private final ThreadID threadID;
  private final int      lockLevel;
  private final String   lockType;
  private final int      hashCode;

  public LockRequest(LockID lockID, ThreadID threadID, int lockLevel) {
    this(lockID, threadID, lockLevel, LockContextInfo.NULL_LOCK_OBJECT_TYPE);
  }

  public LockRequest(LockID lockID, ThreadID threadID, int lockLevel, String lockType) {
    this.lockID = lockID;
    this.threadID = threadID;
    this.lockLevel = lockLevel;
    this.lockType = lockType;
    hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(threadID).append(lockLevel).toHashCode();
  }

  public LockID lockID() {
    return lockID;
  }

  public ThreadID threadID() {
    return threadID;
  }

  public int lockLevel() {
    return lockLevel;
  }

  public String lockType() {
    return lockType;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockRequest)) return false;
    LockRequest cmp = (LockRequest) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && lockLevel == cmp.lockLevel;
  }

  public int hashCode() {
    return hashCode;
  }

  public String toString() {
    return getClass().getName() + "[" + lockID + ", " + threadID + ", lockLevel=" + lockLevel + "]";
  }

}
