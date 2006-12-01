/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.Serializable;

public class ServerLockRequest implements Serializable {

  private final long      requestTime;
  private final ChannelID channelID;
  private final ThreadID  threadID;
  private final String    lockLevel;
  private final String    channelAddr;

  public ServerLockRequest(ChannelID channelID, String channelAddr, ThreadID threadID, int level, long requestTime) {
    this.channelAddr = channelAddr;
    this.channelID = channelID;
    this.threadID = threadID;
    this.requestTime = requestTime;
    this.lockLevel = LockLevel.toString(level);
  }

  public String getChannelAddr() {
    return this.channelAddr;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public String getLockLevel() {
    return lockLevel;
  }

  public long getRequestTime() {
    return requestTime;
  }

  public ThreadID getThreadID() {
    return threadID;
  }
}
