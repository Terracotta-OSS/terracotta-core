/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.LockElementWrapper;
import com.tc.management.L2LockStatsManagerImpl.LockStat;

public class LockStatWrapper implements LockElementWrapper {
  private LockStat lockStat;
  private String lockId;
  private String stackTrace;

  public LockStatWrapper(LockStat lockStat) {
    this.lockStat = lockStat;
    this.lockId = lockStat.getLockID().asString();
    if(this.lockId.charAt(0) != '@') {
      this.lockId = "^"+this.lockId;
    }
  }

  public String getLockID() { return lockId; }
  public long getNumOfLockRequested() { return lockStat.getNumOfLockRequested(); }
  public long getNumOfLockReleased() { return lockStat.getNumOfLockReleased(); }
  public long getNumOfPendingRequests() { return lockStat.getNumOfPendingRequests(); }
  public long getNumOfPendingWaiters() { return lockStat.getNumOfPendingWaiters(); }
  public long getNumOfPingPongRequests() { return lockStat.getNumOfLockHopRequests(); }
  
  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }
  
  public String getStackTrace() {
    return this.stackTrace;
  }
}


