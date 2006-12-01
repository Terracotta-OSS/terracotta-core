/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.Serializable;

public class LockHolder implements Serializable {

  private final ChannelID channelID;
  private final ThreadID  threadID;
  private final long      timeAcquired;
  private final String    lockLevel;
  private final String    channelAddr;

  public LockHolder(ChannelID channelID, String channelAddr, ThreadID threadID, int level, long timeAcquired) {
    this.channelID = channelID;
    this.channelAddr = channelAddr;
    this.threadID = threadID;
    this.timeAcquired = timeAcquired;
    this.lockLevel = LockLevel.toString(level);
  }

  public String getLockLevel() {
    return this.lockLevel;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public String getChannelAddr() {
    return this.channelAddr;
  }

  public long getTimeAcquired() {
    return timeAcquired;
  }

  public ThreadID getThreadID() {
    return threadID;
  }
}
