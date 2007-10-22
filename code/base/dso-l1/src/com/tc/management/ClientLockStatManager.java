/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.net.DSOClientMessageChannel;

public interface ClientLockStatManager {
  public final static ClientLockStatManager NULL_CLIENT_LOCK_STAT_MANAGER = new ClientLockStatManager() {

    public void recordStackTrace(LockID lockID) {
      // do nothing
    }

    public void enableStackTrace(LockID lockID, int lockStackTraceDepth, int lockStatCollectFrequency) {
      // do nothing
    }

    public boolean isStatEnabled(LockID lockID) {
      return false;
    }

    public void disableStackTrace(LockID lockID) {
      // do nothing
    }

    public void start(DSOClientMessageChannel channel, Sink sink) {
      // do nothing
    }
  };
  
  public void start(DSOClientMessageChannel channel, Sink sink);
  
  public void recordStackTrace(LockID lockID);
  
  public void enableStackTrace(LockID lockID, int lockStackTraceDepth, int lockStatCollectFrequency);
  
  public void disableStackTrace(LockID lockID);
  
  public boolean isStatEnabled(LockID lockID);
}
