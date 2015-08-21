/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.net.NodeID;

public class GroupEvent {
  private final NodeID  nodeID;
  private final boolean joined;

  public GroupEvent(NodeID nodeID, boolean joined) {
    this.nodeID = nodeID;
    this.joined = joined;
  }

  public boolean nodeJoined() {
    return joined;
  }

  public NodeID getNodeID() {
    return nodeID;
  }
}
