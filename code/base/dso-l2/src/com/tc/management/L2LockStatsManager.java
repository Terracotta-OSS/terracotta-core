/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.TCStackTraceElement;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;

import java.util.Collection;
import java.util.Collections;

public interface L2LockStatsManager {
  public final static L2LockStatsManager NULL_LOCK_STATS_MANAGER = new L2LockStatsManager() {
    public void start(DSOChannelManager channelManager) {
      // do nothing
    }
    
    public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
      // do nothing
    }
    
    public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, String lockType, int numberOfPendingRequests) {
      // do nothing
    }
    
    public void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp) {
      // do nothing
    }
    
    public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
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
    
    public void recordClientStat(NodeID nodeID, Collection<TCStackTraceElement> lockStatElements) {
      // do nothing
    }
    
    public int getTraceDepth() {
      return 0;
    }

    public int getGatherInterval() {
      return 0;
    }
    
    public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
      // do nothing
    }

    public boolean isLockStatisticsEnabled() {
      return false;
    }
    
    public void clearAllStatsFor(NodeID nodeID) {
      //
    }
    
    public void enableStatsForNodeIfNeeded(NodeID nodeID) {
      //
    }

    public void recordLockHopRequested(LockID lockID) {
      //
    }
    
    public Collection<LockSpec> getLockSpecs() {
      return Collections.EMPTY_LIST;
    }
    
  };
  
  public void start(DSOChannelManager channelManager);
  
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval);
  
  public void recordLockHopRequested(LockID lockID);
  
  public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, String lockType, int numberOfPendingRequests);
  
  public void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp);
  
  public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void recordClientStat(NodeID nodeID, Collection<TCStackTraceElement> lockStatElements);
  
  public long getNumberOfLockRequested(LockID lockID);
  
  public long getNumberOfLockReleased(LockID lockID);
  
  public long getNumberOfPendingRequests(LockID lockID);
  
  public long getNumberOfLockHopRequests(LockID lockID);
  
  public Collection<LockSpec> getLockSpecs();
  
  public int getTraceDepth();
  
  public int getGatherInterval();
  
  public void setLockStatisticsEnabled(boolean lockStatsEnabled);

  public boolean isLockStatisticsEnabled();
  
  public void clearAllStatsFor(NodeID nodeID);
  
  public void enableStatsForNodeIfNeeded(NodeID nodeID);
}
