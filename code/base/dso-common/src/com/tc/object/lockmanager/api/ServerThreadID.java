/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;

public class ServerThreadID {
  public static final ServerThreadID NULL_ID = new ServerThreadID(ClientID.NULL_ID, ThreadID.NULL_ID);

  private final NodeID               nodeID;
  private final ThreadID             threadID;
  private final int                  hashCode;

  public ServerThreadID(NodeID nid, ThreadID threadID) {
    this.nodeID = nid;
    this.threadID = threadID;

    int hash = 31;
    hash = (37 * hash) + nid.hashCode();
    hash = (37 * hash) + threadID.hashCode();
    this.hashCode = hash;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public ThreadID getClientThreadID() {
    return threadID;
  }

  public String toString() {
    return new StringBuffer().append("ServerThreadID{").append(nodeID).append(',').append(threadID).append('}')
        .toString();
  }

  public int hashCode() {
    return this.hashCode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerThreadID) {
      ServerThreadID other = (ServerThreadID) obj;
      return this.nodeID.equals(other.nodeID) && this.threadID.equals(other.threadID);
    }
    return false;
  }
}
