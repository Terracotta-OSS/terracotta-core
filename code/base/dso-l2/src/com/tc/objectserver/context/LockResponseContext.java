/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.lockmanager.impl.GlobalLockStateInfo;
import com.tc.objectserver.lockmanager.api.LockWaitContext;
import com.tc.objectserver.lockmanager.impl.Holder;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class LockResponseContext implements EventContext {

  public static final int      LOCK_AWARD         = 1;
  public static final int      LOCK_RECALL        = 2;
  public static final int      LOCK_WAIT_TIMEOUT  = 3;
  public static final int      LOCK_INFO          = 4;
  public static final int      LOCK_NOT_AWARDED   = 5;
  public static final int      LOCK_STAT_ENABLED  = 6;
  public static final int      LOCK_STAT_DISABLED = 7;

  private final LockID         lockID;
  private final ThreadID       threadID;
  private final int            level;
  private final NodeID         nodeID;
  private final int            responseType;
  private final int            stackTraceDepth;
  private final int            statCollectFrequency;
  private final GlobalLockInfo globalLockInfo;

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID threadID, int level, int lockRequestQueueLength,
                             Collection greedyHolders, Collection holders, Collection waiters, int type) {
    this(lockID, nodeID, threadID, level, new GlobalLockInfo(lockID, level, lockRequestQueueLength,
                                                             getGlobalLockHolderInfo(greedyHolders),
                                                             getGlobalLockHolderInfo(holders),
                                                             getGlobalLockWaiterInfo(lockID, waiters)), type, -1, -1);
  }

  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, int level, int type) {
    this(lockID, nodeID, sourceID, level, null, type, -1, -1);
  }
  
  public LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, int level, int type, int stackTraceDepth, int statCollectFrequency) {
    this(lockID, nodeID, sourceID, level, null, type, stackTraceDepth, statCollectFrequency);
  }

  private LockResponseContext(LockID lockID, NodeID nodeID, ThreadID sourceID, int level,
                              GlobalLockInfo globalLockInfo, int type, int stackTraceDepth, int statCollectFrequency) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = sourceID;
    this.level = level;
    this.responseType = type;
    this.globalLockInfo = globalLockInfo;
    this.stackTraceDepth = stackTraceDepth;
    this.statCollectFrequency = statCollectFrequency;
    Assert.assertTrue(responseType == LOCK_AWARD || responseType == LOCK_RECALL || responseType == LOCK_WAIT_TIMEOUT
                      || responseType == LOCK_INFO || responseType == LOCK_NOT_AWARDED
                      || responseType == LOCK_STAT_ENABLED || responseType == LOCK_STAT_DISABLED);
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public int getType() {
    return level;
  }

  public LockID getLockID() {
    return lockID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public int getLockLevel() {
    return level;
  }

  public int getStackTraceDepth() {
    return stackTraceDepth;
  }

  public int getStatCollectFrequency() {
    return statCollectFrequency;
  }

  public GlobalLockInfo getGlobalLockInfo() {
    return globalLockInfo;
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

  public boolean isLockStatEnabled() {
    return (this.responseType == LOCK_STAT_ENABLED);
  }

  public boolean isLockStatDisabled() {
    return (this.responseType == LOCK_STAT_DISABLED);
  }

  public String toString() {
    return "LockResponseContext(" + lockID + "," + nodeID + "," + threadID + ", " + LockLevel.toString(level) + " , "
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

  private static Collection getGlobalLockHolderInfo(Collection holderInfo) {
    Collection holdersInfo = new ArrayList();
    for (Iterator i = holderInfo.iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      holdersInfo.add(new GlobalLockStateInfo(holder.getLockID(), holder.getNodeID(), holder.getThreadID(), holder
          .getTimestamp(), holder.getTimeout(), holder.getLockLevel()));
    }
    return holdersInfo;
  }

  private static Collection getGlobalLockWaiterInfo(LockID id, Collection waiters) {
    Collection waitersInfo = new ArrayList();
    for (Iterator i = waiters.iterator(); i.hasNext();) {
      LockWaitContext lockWaitContext = (LockWaitContext) i.next();
      waitersInfo.add(new GlobalLockStateInfo(id, lockWaitContext.getNodeID(), lockWaitContext.getThreadID(),
                                              lockWaitContext.getTimestamp(), -1, lockWaitContext.lockLevel()));
    }
    return waitersInfo;
  }
}
