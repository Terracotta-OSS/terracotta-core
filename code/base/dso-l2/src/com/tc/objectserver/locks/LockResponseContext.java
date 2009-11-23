/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.util.Assert;

import java.util.Collection;

public class LockResponseContext implements MultiThreadedEventContext {

  private static final int                                  LOCK_NO_LEASE     = 0;

  public static final int                                   LOCK_AWARD        = 1;
  public static final int                                   LOCK_RECALL       = 2;
  public static final int                                   LOCK_WAIT_TIMEOUT = 3;
  public static final int                                   LOCK_INFO         = 4;
  public static final int                                   LOCK_NOT_AWARDED  = 5;

  private final LockID                                      lockID;
  private final ThreadID                                    threadID;
  private final ServerLockLevel                             level;
  private final NodeID                                      nodeID;
  private final int                                         responseType;
  private final Collection<ClientServerExchangeLockContext> contexts;
  private final int                                         noOfPendingRequests;
  private int                                               leaseTimeInMs     = LOCK_NO_LEASE;

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID threadID, ServerLockLevel level,
                             Collection<ClientServerExchangeLockContext> contexts, int numberOfPending, int type) {
    this(lockID, nodeID, threadID, level, contexts, numberOfPending, type, LOCK_NO_LEASE);
  }

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, ServerLockLevel level, int type) {
    this(lockID, nodeID, sourceID, level, null, 0, type, LOCK_NO_LEASE);
  }

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, ServerLockLevel level, int type,
                             int leaseTimeInMs) {
    this(lockID, nodeID, sourceID, level, null, 0, type, leaseTimeInMs);
  }

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, ServerLockLevel level, int type,
                             int traceDepth, int gatherInterval) {
    this(lockID, nodeID, sourceID, level, null, 0, type, LOCK_NO_LEASE);
  }

  private LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, ServerLockLevel level,
                              Collection<ClientServerExchangeLockContext> contexts, int noOfPendingRequests, int type,
                              int leaseTimeInMs) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = sourceID;
    this.level = level;
    this.responseType = type;
    this.contexts = contexts;
    this.noOfPendingRequests = noOfPendingRequests;
    this.leaseTimeInMs = leaseTimeInMs;
    Assert.assertTrue(responseType == LOCK_AWARD || responseType == LOCK_RECALL || responseType == LOCK_WAIT_TIMEOUT
                      || responseType == LOCK_INFO || responseType == LOCK_NOT_AWARDED);
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public ServerLockLevel getLockLevel() {
    return level;
  }

  public Collection<ClientServerExchangeLockContext> getGlobalLockInfo() {
    return contexts;
  }

  public int getNumberOfPendingRequests() {
    return noOfPendingRequests;
  }

  public boolean isLockAward() {
    return (this.responseType == LOCK_AWARD);
  }

  public boolean isLockRecall() {
    return (this.responseType == LOCK_RECALL);
  }

  public boolean isLockWaitTimeout() {
    return (this.responseType == LOCK_WAIT_TIMEOUT);
  }

  public boolean isLockInfo() {
    return (this.responseType == LOCK_INFO);
  }

  public boolean isLockNotAwarded() {
    return (this.responseType == LOCK_NOT_AWARDED);
  }

  public int getAwardLeaseTime() {
    return this.leaseTimeInMs;
  }

  @Override
  public String toString() {
    return "LockResponseContext(" + lockID + "," + nodeID + "," + threadID + ", " + level + " , "
           + toString(responseType) + ")";
  }

  private static String toString(int responseType2) {
    switch (responseType2) {
      case LOCK_AWARD:
        return "LOCK_AWARD";
      case LOCK_RECALL:
        return "LOCK_RECALL";
      case LOCK_WAIT_TIMEOUT:
        return "LOCK_WAIT_TIMEOUT";
      case LOCK_INFO:
        return "LOCK_INFO";
      case LOCK_NOT_AWARDED:
        return "LOCK_NOT_AWARDED";
      default:
        return "UNKNOWN";
    }
  }

  public Object getKey() {
    return nodeID;
  }
}
