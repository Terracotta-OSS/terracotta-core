/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.LockManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface L2LockStatsManager {
  public final static L2LockStatsManager NULL_LOCK_STATS_MANAGER = new L2LockStatsManager() {
    public void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
      // do nothing
    }
    
    public void enableClientStat(LockID lockID) {
      // do nothing
    }
    
    public void enableClientStat(LockID lockID, int stackTraceDepth, int statCollectFrequency) {
      // do nothing
    }
    
    public void disableClientStat(LockID lockID) {
      // do nothing
    }
    
    public boolean isClientLockStatEnable(LockID lockID) {
      return false;
    }
    
    public void lockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel) {
      // do nothing
    }
    
    public void lockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp) {
      // do nothing
    }
    
    public void lockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public void lockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public void lockWait(LockID lockID) {
      // do nothing
    }
    
    public void lockNotified(LockID lockID, int n) {
      // do nothing
    }
    
    public long getNumberOfLockRequested(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfLockReleased(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfPendingRequests(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfLockHopRequests(LockID lockID) {
      return 0;
    }
    
    public LockHolder getLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID) {
      return null;
    }
    
    public Collection getTopLockStats(int n) {
      return Collections.EMPTY_LIST;
    }
    
    public Collection getTopLockHoldersStats(int n) {
      return Collections.EMPTY_LIST;
    }
    
    public Collection getTopWaitingLocks(int n) {
      return Collections.EMPTY_LIST;
    }
    
    public Collection getTopContendedLocks(int n) {
      return Collections.EMPTY_LIST;
    }
    
    public Collection getTopLockHops(int n) {
      return Collections.EMPTY_LIST;
    }

    public void recordStackTraces(LockID lockID, NodeID nodeID, List stackTraces) {
      // do nothing
    }
    
    public Collection getStackTraces(LockID lockID) {
      return Collections.EMPTY_LIST;
    }

    public boolean isLockStatEnabledInClient(LockID lockID, NodeID nodeID) {
      return false;
    }

    public void recordClientStatEnabled(LockID lockID, NodeID nodeID) {
      // do nothing
    }

    public int getLockStackTraceDepth(LockID lockID) {
      return 0;
    }

    public int getLockStatCollectFrequency(LockID lockID) {
      return 0;
    }
    
    public void enableLockStatistics() {
      // do nothing
    }
    
    public void disableLockStatistics() {
      // do nothing
    }
  };
  
  public void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink);
  
  public void enableClientStat(LockID lockID);
  
  public void enableClientStat(LockID lockID, int stackTraceDepth, int statCollectFrequency);
  
  public void disableClientStat(LockID lockID);
  
  public boolean isClientLockStatEnable(LockID lockID);
  
  public boolean isLockStatEnabledInClient(LockID lockID, NodeID nodeID);
  
  public void recordClientStatEnabled(LockID lockID, NodeID nodeID);
  
  public void lockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel);

  public void lockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp);
  
  public void lockReleased(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void lockRejected(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void lockWait(LockID lockID);
  
  public void lockNotified(LockID lockID, int n);
  
  public void recordStackTraces(LockID lockID, NodeID nodeID, List stackTraces);
  
  public long getNumberOfLockRequested(LockID lockID);
  
  public long getNumberOfLockReleased(LockID lockID);
  
  public long getNumberOfPendingRequests(LockID lockID);
  
  public long getNumberOfLockHopRequests(LockID lockID);
  
  public LockHolder getLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public Collection getTopLockStats(int n);
  
  public Collection getTopLockHoldersStats(int n);
  
  public Collection getTopWaitingLocks(int n);
  
  public Collection getTopContendedLocks(int n);
  
  public Collection getTopLockHops(int n);
  
  public Collection getStackTraces(LockID lockID);
  
  public int getLockStackTraceDepth(LockID lockID);
  
  public int getLockStatCollectFrequency(LockID lockID);
  
  public void enableLockStatistics();
  
  public void disableLockStatistics();
}
