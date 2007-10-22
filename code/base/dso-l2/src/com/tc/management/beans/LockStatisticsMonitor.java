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
  
  public Collection getTopAggregateLockHolderStats(int n) {
    return this.lockStatsManager.getTopAggregateLockHolderStats(n);
  }

  public Collection getTopRequested(int n) {
    return this.lockStatsManager.getTopLockStats(n);
  }
  
  public Collection getTopWaitingLocks(int n) {
    return this.lockStatsManager.getTopWaitingLocks(n);
  }
  
  public Collection getTopAggregateWaitingLocks(int n) {
    return this.lockStatsManager.getTopAggregateWaitingLocks(n);
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
  
  public void enableClientStackTrace(String lockID) {
    this.lockStatsManager.enableClientStackTrace(new LockID(lockID));
  }
  
  public void enableClientStackTrace(String lockID, int stackTraceDepth, int statCollectFrequency) {
    this.lockStatsManager.enableClientStackTrace(new LockID(lockID), stackTraceDepth, statCollectFrequency);
  }
  
  public void disableClientStackTrace(String lockID) {
    this.lockStatsManager.disableClientStackTrace(new LockID(lockID));
  }

  public boolean isClientStackTraceEnabled(String lockID) {
    return this.lockStatsManager.isClientLockStackTraceEnable(new LockID(lockID));
  }
  
  public void enableLockStatistics() {
    this.lockStatsManager.enableLockStatistics();
  }
  
  public void disableLockStatistics() {
    this.lockStatsManager.disableLockStatistics();
  }
}
