/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.Serializable;

public class LockHolder implements Serializable {
  private final static long NON_SET_TIME_MILLIS = -1;

  private final LockID      lockID;
  private final NodeID      nodeID;
  private final ThreadID    threadID;
  private final String      lockLevel;
  private final String      channelAddr;
  private long              timeAcquired;
  private long              timeReleased;
  private long              timeRequested;
  private long              waitTimeInMillis;
  private long              heldTimeInMillis;

  public LockHolder(LockID lockID, NodeID cid, String channelAddr, ThreadID threadID, int level, long timeRequested) {
    this.lockID = lockID;
    this.nodeID = cid;
    this.channelAddr = channelAddr;
    this.threadID = threadID;
    this.timeRequested = timeRequested;
    this.waitTimeInMillis = NON_SET_TIME_MILLIS;
    this.heldTimeInMillis = NON_SET_TIME_MILLIS;
    this.lockLevel = LockLevel.toString(level);
  }

  public LockHolder(LockID lockID, NodeID cid, String channelAddr, ThreadID threadID, int level) {
    this(lockID, cid, channelAddr, threadID, level, NON_SET_TIME_MILLIS);
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public String getLockLevel() {
    return this.lockLevel;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public String getChannelAddr() {
    return this.channelAddr;
  }

  public long getTimeAcquired() {
    return timeAcquired;
  }

  public long getTimeReleased() {
    return timeReleased;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void lockAcquired(long timeAcquired) {
    if (timeRequested <= 0) {
      this.timeRequested = timeAcquired;
    }
    this.timeAcquired = timeAcquired;
  }

  public void lockReleased() {
    this.timeReleased = System.currentTimeMillis();
    if (timeAcquired <= 0) {
      timeAcquired = timeReleased;
    }
  }

  public long getWaitTimeInMillis() {
    if (waitTimeInMillis == NON_SET_TIME_MILLIS) {
      getAndSetWaitTimeInMillis();
    }
    return waitTimeInMillis;
  }

  public long getHeldTimeInMillis() {
    if (heldTimeInMillis == NON_SET_TIME_MILLIS) {
      getAndSetHeldTimeInMillis();
    }
    return heldTimeInMillis;
  }

  public long getAndSetWaitTimeInMillis() {
    if (timeAcquired <= 0 && timeRequested <= 0) {
      waitTimeInMillis = 0;
    } else if (timeAcquired <= 0) {
      waitTimeInMillis = System.currentTimeMillis() - timeRequested;
    } else {
      waitTimeInMillis = timeAcquired - timeRequested;
    }
    return waitTimeInMillis;
  }

  public long getAndSetHeldTimeInMillis() {
    if (timeReleased <= 0 && timeAcquired <= 0) {
      heldTimeInMillis = 0;
    } else if (timeReleased <= 0) {
      heldTimeInMillis = System.currentTimeMillis() - timeAcquired;
    } else {
      heldTimeInMillis = timeReleased - timeAcquired;
    }

    return heldTimeInMillis;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[Lock ID: ");
    sb.append(lockID);
    sb.append(", Node ID: ");
    sb.append(nodeID);
    sb.append(", Thread ID: ");
    sb.append(threadID);
    sb.append(", lock level: ");
    sb.append(lockLevel);
    sb.append(", held time in millis: ");
    sb.append(getAndSetHeldTimeInMillis());
    sb.append(", wait time in millis: ");
    sb.append(getAndSetWaitTimeInMillis());
    sb.append(", time acquired: ");
    sb.append(timeAcquired);
    sb.append(", time released: ");
    sb.append(timeReleased);
    sb.append("]");
    return sb.toString();
  }
}
