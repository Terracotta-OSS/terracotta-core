/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;


public class QueryLockRequest {
  private LockID   lockID;
  private ThreadID threadID;
  private Object   waitLock;

  public QueryLockRequest(LockID lockID, ThreadID threadID, Object waitLock) {
    this.lockID = lockID;
    this.threadID = threadID;
    this.waitLock = waitLock;
  }

  public LockID lockID() {
    return lockID;
  }

  public ThreadID threadID() {
    return threadID;
  }
  
  public Object getWaitLock() {
    return this.waitLock;
  }

  public String toString() {
    return getClass().getName() + "[" + lockID + ", " + threadID + "]";
  }

}
