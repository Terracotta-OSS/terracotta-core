/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * Client/Server intermediate fromat for holding the context of a lock request/award. This class bridges the types used
 * internally by the ClientLockManager and the server LockManager so they can be sent in messages back and forth to each
 * other.
 */
public class LockContext implements TCSerializable {

  private LockID    lockID;
  private int       lockLevel;
  private ChannelID channelID;
  private ThreadID  threadID;
  private boolean   noBlock;
  private int       hashCode;

  public LockContext() {
    return;
  }
  
  public LockContext(LockID lockID, ChannelID channelID, ThreadID threadID, int lockLevel, boolean noBlock) {
    this.lockID = lockID;
    this.channelID = channelID;
    this.threadID = threadID;
    Assert.assertFalse(LockLevel.isSynchronous(lockLevel));
    this.lockLevel = lockLevel;
    this.noBlock = noBlock;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(channelID).append(threadID).append(lockLevel).append(noBlock)
        .toHashCode();
  }

  public LockContext(LockID lockID, ChannelID channelID, ThreadID threadID, int lockLevel) {
    this(lockID, channelID, threadID, lockLevel, false);
  }
  
  public ChannelID getChannelID() {
    return channelID;
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
  
  public boolean noBlock() {
    return noBlock;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockContext)) return false;
    LockContext cmp = (LockContext) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && lockLevel == cmp.lockLevel
           && channelID.equals(cmp.channelID) && noBlock == cmp.noBlock;
  }

  public int hashCode() {
    return hashCode;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(lockID.asString());
    output.writeLong(channelID.toLong());
    output.writeLong(threadID.toLong());
    output.writeInt(lockLevel);
    output.writeBoolean(noBlock);
  }

  public Object deserializeFrom(TCByteBufferInputStream input) throws IOException {
    lockID = new LockID(input.readString());
    channelID = new ChannelID(input.readLong());
    threadID = new ThreadID(input.readLong());
    lockLevel = input.readInt();
    noBlock = input.readBoolean();
    return this;
  }

}
