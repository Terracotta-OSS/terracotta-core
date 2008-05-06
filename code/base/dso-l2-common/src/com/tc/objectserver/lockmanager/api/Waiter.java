/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.TimerSpec;

import java.io.Serializable;

public class Waiter implements Serializable {

  private final long     startTime;
  private final NodeID   nodeID;
  private final ThreadID threadID;
  private final String   waitInvocation;
  private final String   channelAddr;

  public Waiter(NodeID cid, String channelAddr, ThreadID threadID, TimerSpec call, long startTime) {
    this.nodeID = cid;
    this.channelAddr = channelAddr;
    this.threadID = threadID;
    this.startTime = startTime;
    this.waitInvocation = call.toString();
  }

  public NodeID getNodeID() {
    return nodeID;
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
