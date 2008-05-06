/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.IOException;

/**
 * This class is used to hold the global holder/waiter information of a lock.
 */
public class GlobalLockStateInfo implements TCSerializable {
  private LockID   lockID;
  private NodeID   nodeID;
  private ThreadID threadID;
  private long     timeout;
  private long     timestamp;
  private int      lockLevel;

  public GlobalLockStateInfo() {
    super();
  }

  public GlobalLockStateInfo(LockID lockID, NodeID nodeID, ThreadID threadID, long timestamp, long timeout,
                             int lockLevel) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = threadID;
    this.timestamp = timestamp;
    this.timeout = timeout;
    this.lockLevel = lockLevel;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public int getLockLevel() {
    return lockLevel;
  }

  public long getTimeout() {
    return timeout;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(timestamp);
    serialOutput.writeString(lockID.asString());
    NodeIDSerializer ns = new NodeIDSerializer(nodeID);
    ns.serializeTo(serialOutput);
    serialOutput.writeLong(threadID.toLong());
    serialOutput.writeLong(timeout);
    serialOutput.writeInt(lockLevel);
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.timestamp = serialInput.readLong();
    this.lockID = new LockID(serialInput.readString());
    NodeIDSerializer ns = new NodeIDSerializer();
    ns.deserializeFrom(serialInput);
    this.nodeID = ns.getNodeID();
    this.threadID = new ThreadID(serialInput.readLong());
    this.timeout = serialInput.readLong();
    this.lockLevel = serialInput.readInt();
    return this;
  }

}