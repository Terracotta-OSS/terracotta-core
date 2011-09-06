/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;

import java.io.Serializable;

public class LockHolder implements Serializable {
  private final static long NON_SET_TIME_MILLIS = -1;

  private final LockID      lockID;
  private final ThreadID    threadID;
  private final String      channelAddr;
  private long              timeAcquired;
  private long              timeReleased;
  private long              timeRequested;
  private long              waitTimeInMillis;
  private long              heldTimeInMillis;

  public LockHolder(LockID lockID, String channelAddr, ThreadID threadID, long timeRequested) {
    this.lockID = lockID;
    this.channelAddr = channelAddr;
    this.threadID = threadID;
    this.timeRequested = timeRequested;
    this.waitTimeInMillis = NON_SET_TIME_MILLIS;
    this.heldTimeInMillis = NON_SET_TIME_MILLIS;
  }

  public LockHolder(LockID lockID, ThreadID threadID, long timeRequested) {
    this(lockID, null, threadID, timeRequested);
  }

  public LockID getLockID() {
    return this.lockID;
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

  public void lockAcquired(long lockTimeAcquired) {
    if (timeRequested <= 0) {
      this.timeRequested = lockTimeAcquired;
    }
    this.timeAcquired = lockTimeAcquired;
    getAndSetWaitTimeInMillis();
  }

  public void lockReleased(long lockTimeReleased) {
    if (timeAcquired <= 0) {
      timeAcquired = lockTimeReleased;
    }
    this.timeReleased = lockTimeReleased;
    getAndSetHeldTimeInMillis();
  }

  public void computeWaitAndHeldTimeInMillis() {
    if (timeAcquired <= 0) {
      getAndSetWaitTimeInMillis();
    } else {
      getAndSetWaitTimeInMillis();
      getAndSetHeldTimeInMillis();
    }
  }

  public long getWaitTimeInMillis() {
    return waitTimeInMillis;
  }

  public long getHeldTimeInMillis() {
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
    sb.append(", Thread ID: ");
    sb.append(threadID);
    sb.append(", held time in millis: ");
    sb.append(getAndSetHeldTimeInMillis());
    sb.append(", wait time in millis: ");
    sb.append(getAndSetWaitTimeInMillis());
    sb.append(", time requested: ");
    sb.append(timeRequested);
    sb.append(", time acquired: ");
    sb.append(timeAcquired);
    sb.append(", time released: ");
    sb.append(timeReleased);
    sb.append(" ");
    sb.append(System.identityHashCode(this));
    sb.append("]");
    return sb.toString();
  }
}
