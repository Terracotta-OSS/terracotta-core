/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCDataOutput;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.IOException;

/**
 * This class is used to hold the global holder/waiter information of a lock.
 */
public class GlobalLockStateInfo {
  private LockID    lockID;
  private ChannelID channelID;
  private ThreadID  threadID;
  private long      timeout;
  private long      timestamp;
  private int       lockLevel;

  public GlobalLockStateInfo() {
    super();
  }

  public GlobalLockStateInfo(LockID lockID, ChannelID channelID, ThreadID threadID, long timestamp, long timeout,
                             int lockLevel) {
    this.lockID = lockID;
    this.channelID = channelID;
    this.threadID = threadID;
    this.timestamp = timestamp;
    this.timeout = timeout;
    this.lockLevel = lockLevel;
  }

  public ChannelID getChannelID() {
    return channelID;
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

  public void serializeTo(TCDataOutput serialOutput) {
    serialOutput.writeLong(timestamp);
    serialOutput.writeString(lockID.asString());
    serialOutput.writeLong(channelID.toLong());
    serialOutput.writeLong(threadID.toLong());
    serialOutput.writeLong(timeout);
    serialOutput.writeInt(lockLevel);
  }

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    this.timestamp = serialInput.readLong();
    this.lockID = new LockID(serialInput.readString());
    this.channelID = new ChannelID(serialInput.readLong());
    this.threadID = new ThreadID(serialInput.readLong());
    this.timeout = serialInput.readLong();
    this.lockLevel = serialInput.readInt();
    return this;
  }

}