/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.L2LockStatsManager;
import com.tc.object.lockmanager.api.LockID;

import java.io.Serializable;
import java.util.Collection;

public class LockStatisticsMonitor implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager lockStatsManager;
  
  public LockStatisticsMonitor(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public Collection getTopHeld(int n) {
    return this.lockStatsManager.getTopLockHoldersStats(n);
  }

  public Collection getTopRequested(int n) {
    return this.lockStatsManager.getTopLockStats(n);
  }
  
  public Collection getTopWaitingLocks(int n) {
    return this.lockStatsManager.getTopWaitingLocks(n);
  }
  
  public Collection getTopContendedLocks(int n) {
    return this.lockStatsManager.getTopContendedLocks(n);
  }
  
  public Collection getTopLockHops(int n) {
    return this.lockStatsManager.getTopLockHops(n);
  }
  
  public Collection getStackTraces(String lockID) {
    return this.lockStatsManager.getStackTraces(new LockID(lockID));
  }
  
  public void enableClientStat(String lockID) {
    this.lockStatsManager.enableClientStat(new LockID(lockID));
  }
  
  public void enableClientStat(String lockID, int stackTraceDepth, int statCollectFrequency) {
    this.lockStatsManager.enableClientStat(new LockID(lockID), stackTraceDepth, statCollectFrequency);
  }
  
  public void disableClientStat(String lockID) {
    this.lockStatsManager.disableClientStat(new LockID(lockID));
  }

  public boolean isClientStatEnabled(String lockID) {
    return this.lockStatsManager.isClientLockStatEnable(new LockID(lockID));
  }
  
  public void enableLockStatistics() {
    this.lockStatsManager.enableLockStatistics();
  }
  
  public void disableLockStatistics() {
    this.lockStatsManager.disableLockStatistics();
  }
}
