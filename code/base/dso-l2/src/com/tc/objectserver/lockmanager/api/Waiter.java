/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;

import java.io.Serializable;

public class Waiter implements Serializable {

  private final long      startTime;
  private final ChannelID channelID;
  private final ThreadID  threadID;
  private final String    waitInvocation;
  private final String    channelAddr;

  public Waiter(ChannelID channelID, String channelAddr, ThreadID threadID, WaitInvocation call, long startTime) {
    this.channelID = channelID;
    this.channelAddr = channelAddr;
    this.threadID = threadID;
    this.startTime = startTime;
    this.waitInvocation = call.toString();
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public String getChannelAddr() {
    return this.channelAddr;
  }

  public long getStartTime() {
    return startTime;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public String getWaitInvocation() {
    return waitInvocation;
  }
}
