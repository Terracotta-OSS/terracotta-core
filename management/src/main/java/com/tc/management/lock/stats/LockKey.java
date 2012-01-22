/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.net.NodeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;

public class LockKey {
  private LockID   lockID;
  private NodeID   nodeID;
  private ThreadID threadID;
  private int      hashCode;

  private LockKey  subKey;

  public LockKey(LockID lockID, NodeID nodeID) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = null;
    this.subKey = null;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).toHashCode();
  }
  
  public LockKey(NodeID nodeID, ThreadID threadID) {
    this.lockID = null;
    this.nodeID = nodeID;
    this.threadID = threadID;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(nodeID).append(threadID).toHashCode();
  }

  public LockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = threadID;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).append(threadID).toHashCode();
    this.subKey = new LockKey(lockID, nodeID);
  }

  public String toString() {
    return "LockKey [ " + lockID + ", " + nodeID + ", " + threadID + ", " + hashCode + "] ";
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

  public LockKey subKey() {
    return subKey;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockKey)) return false;
    LockKey cmp = (LockKey) o;
    if (lockID == null) {
      return nodeID.equals(cmp.nodeID) && threadID.equals(cmp.threadID);
    } else if (threadID != null) {
      return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID) && threadID.equals(cmp.threadID);
    } else {
      return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID);
    }
  }

  public int hashCode() {
    return hashCode;
  }
}

