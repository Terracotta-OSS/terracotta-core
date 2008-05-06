/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * Client/Server intermediate fromat for holding the context of a lock request/award. This class bridges the types used
 * internally by the ClientLockManager and the server LockManager so they can be sent in messages back and forth to each
 * other.
 */
public class LockContext implements TCSerializable {

  private LockID   lockID;
  private int      lockLevel;
  private String   lockType;
  private NodeID   nodeID;
  private ThreadID threadID;
  private int      hashCode;

  public LockContext() {
    return;
  }

  public LockContext(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel, String lockType) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = threadID;
    Assert.assertFalse(LockLevel.isSynchronous(lockLevel));
    this.lockLevel = lockLevel;
    this.lockType = lockType;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).append(threadID).append(lockLevel)
        .toHashCode();
  }

  public String toString() {
    return "LockContext [ " + lockID + ", " + LockLevel.toString(lockLevel) + ", " + nodeID + ", " + threadID + ", "
           + hashCode + "] ";
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public int getLockLevel() {
    return this.lockLevel;
  }

  public ThreadID getThreadID() {
    return threadID;
  }
  
  public String getLockType() {
    return lockType;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockContext)) return false;
    LockContext cmp = (LockContext) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && lockLevel == cmp.lockLevel
           && nodeID.equals(cmp.nodeID);
  }

  public int hashCode() {
    return hashCode;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(lockID.asString());
    NodeIDSerializer ns = new NodeIDSerializer(this.nodeID);
    ns.serializeTo(output);
    output.writeLong(threadID.toLong());
    output.writeInt(lockLevel);
    output.writeString(lockType);
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    lockID = new LockID(input.readString());
    NodeIDSerializer ns = new NodeIDSerializer();
    ns.deserializeFrom(input);
    nodeID = (ClientID) ns.getNodeID();
    threadID = new ThreadID(input.readLong());
    lockLevel = input.readInt();
    lockType = input.readString();
    return this;
  }

}
