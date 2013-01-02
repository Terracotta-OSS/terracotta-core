/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNotSupportedMethodException;
import com.tc.net.NodeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;

import java.io.Serializable;
import java.util.Collection;

public class ClientLockStatisticsInfoImpl implements LockSpec, LockStatisticsInfo, Serializable {
  private final LockID          lockID;
  private int                   gatherInterval;
  private int                   recordedFrequency;

  private final LockStatElement statElement;
  private final LockStats       stat;

  public ClientLockStatisticsInfoImpl(LockID lockID, int gatherInterval) {
    this.lockID = lockID;
    this.gatherInterval = gatherInterval;
    statElement = new LockStatElement(lockID, null);
    stat = statElement.getStats();
  }

  @Override
  public void recordLockRequested(NodeID nodeID, ThreadID threadID, long requestTimeInMillis,
                                  int numberOfPendingRequests, StackTraceElement[] stackTraces, String contextInfo) {
    statElement.recordLockRequested(nodeID, threadID, requestTimeInMillis, numberOfPendingRequests, contextInfo,
                                    stackTraces, 0);
    if (gatherInterval > 0) {
      this.recordedFrequency = (this.recordedFrequency + 1) % gatherInterval;
    }
  }

  public int getRecordedFrequency() {
    return recordedFrequency;
  }

  @Override
  public boolean recordLockAwarded(NodeID nodeID, ThreadID threadID, boolean isGreedy, long awardedTimeInMillis,
                                   int nestedLockDepth) {
    boolean rv = statElement.recordLockAwarded(nodeID, threadID, isGreedy, awardedTimeInMillis, nestedLockDepth);
    if (gatherInterval > 0) {
      this.recordedFrequency = (this.recordedFrequency + 1) % gatherInterval;
    }
    return rv;
  }

  @Override
  public boolean recordLockReleased(NodeID nodeID, ThreadID threadID) {
    boolean rv = statElement.recordLockReleased(nodeID, threadID);
    if (gatherInterval > 0) {
      this.recordedFrequency = (this.recordedFrequency + 1) % gatherInterval;
    }
    return rv;
  }

  public void recordLockHopRequested(NodeID nodeID, ThreadID threadID, StackTraceElement[] stackTraces) {
    statElement.recordLockHopped(nodeID, threadID, stackTraces, 0);
    if (gatherInterval > 0) {
      this.recordedFrequency = (this.recordedFrequency + 1) % gatherInterval;
    }
  }

  @Override
  public void recordLockRejected(NodeID nodeID, ThreadID threadID) {
    statElement.recordLockRejected(nodeID, threadID);
    if (gatherInterval > 0) {
      this.recordedFrequency = (this.recordedFrequency + 1) % gatherInterval;
    }
  }

  @Override
  public void aggregateLockHoldersData() {
    statElement.aggregateLockHoldersData(stat, 0);
  }

  @Override
  public LockID getLockID() {
    return lockID;
  }

  @Override
  public LockStats getServerStats() {
    throw new TCNotSupportedMethodException();
  }

  @Override
  public LockStats getClientStats() {
    return stat;
  }

  @Override
  public long getNumberOfLockRequested() {
    return -1;
  }

  @Override
  public long getNumberOfLockHopRequested() {
    return -1;
  }

  @Override
  public long getNumberOfLockReleased() {
    return stat.getNumOfLockReleased();
  }

  @Override
  public long getNumberOfPendingRequests() {
    return stat.getNumOfLockPendingRequested();
  }

  @Override
  public boolean hasChildren() {
    return statElement.hasChildren();
  }

  @Override
  public Collection children() {
    return statElement.children();
  }

  @Override
  public LockStatElement getLockStatElement() {
    return statElement;
  }

  // TODO: return empty string for now
  @Override
  public String getObjectType() {
    return "";
  }

  public void mergeLockStatElements() {
    throw new ImplementMe();
  }

}
