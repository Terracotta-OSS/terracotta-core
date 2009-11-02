/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOClientMessageChannel;

public interface ClientLockStatManager {
  public final static ClientLockStatManager NULL_CLIENT_LOCK_STAT_MANAGER = new ClientLockStatManager() {

    public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
      // do nothing
    }

    public void setLockStatisticsEnabled(boolean statEnable) {
      // do nothing
    }

    public void start(DSOClientMessageChannel channel, Sink sink) {
      // do nothing
    }

    public void recordLockRequested(LockID lockID, ThreadID threadID, String contextInfo, int numberOfPendingLockRequests) {
      // do nothing
    }

    public void recordLockAwarded(LockID lockID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockReleased(LockID lockID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockHopped(LockID lockID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockRejected(LockID lockID, ThreadID threadID) {
      // do nothing
    }
    
    public void requestLockSpecs(NodeID nodeID) {
      // do nothing
    }
    
    public boolean isEnabled() {
      return false;
    }
  };
  
  public void start(DSOClientMessageChannel channel, Sink sink);
  
  public void recordLockRequested(LockID lockID, ThreadID threadID, String contextInfo, int numberOfPendingLockRequests);
  
  public void recordLockAwarded(LockID lockID, ThreadID threadID);
  
  public void recordLockReleased(LockID lockID, ThreadID threadID);
  
  public void recordLockHopped(LockID lockID, ThreadID threadID);
  
  public void recordLockRejected(LockID lockID, ThreadID threadID);
  
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval);
  
  public void setLockStatisticsEnabled(boolean statEnable);
  
  public void requestLockSpecs(NodeID nodeID);
  
  public boolean isEnabled();
}
