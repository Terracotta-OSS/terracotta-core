/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class LockRequest {
  private LockID   lockID;
  private ThreadID threadID;
  private int      lockLevel;
  private int      hashCode;
  private boolean  initialized;

  public LockRequest(LockID lockID, ThreadID threadID, int lockLevel) {
    initialize(lockID, threadID, lockLevel);
  }

  private void initialize(LockID theLockID, ThreadID theThreadID, int theLockLevel) {
    if (initialized) throw new AssertionError("Attempt to intialize more than once.");
    this.lockID = theLockID;
    this.threadID = theThreadID;
    this.lockLevel = theLockLevel;
    hashCode = new HashCodeBuilder(5503, 6737).append(theLockID).append(theThreadID).append(theLockLevel).toHashCode();
    initialized = true;
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

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockRequest)) return false;
    LockRequest cmp = (LockRequest) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && lockLevel == cmp.lockLevel;
  }

  public int hashCode() {
    if (!initialized) throw new AssertionError("Attempt to call hashCode() before initializing");
    return hashCode;
  }

  public String toString() {
    return getClass().getName() + "[" + lockID + ", " + threadID + ", lockLevel=" + lockLevel + "]";
  }

}
