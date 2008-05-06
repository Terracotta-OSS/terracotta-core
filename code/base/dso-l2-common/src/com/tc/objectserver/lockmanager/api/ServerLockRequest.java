/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.Serializable;

public class ServerLockRequest implements Serializable {

  private final long     requestTime;
  private final NodeID   nodeID;
  private final ThreadID threadID;
  private final String   lockLevel;
  private final String   channelAddr;

  public ServerLockRequest(NodeID cid, String channelAddr, ThreadID threadID, int level, long requestTime) {
    this.channelAddr = channelAddr;
    this.nodeID = cid;
    this.threadID = threadID;
    this.requestTime = requestTime;
    this.lockLevel = LockLevel.toString(level);
  }

  public String getChannelAddr() {
    return this.channelAddr;
  }

  public NodeID getNodeID() {
    return nodeID;
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
