package com.tc.objectserver.handler;

import com.tc.net.NodeID;

public abstract class ReplicationSenderMessage {
  private final NodeID nodeID;

  public ReplicationSenderMessage(NodeID nodeID) {
    this.nodeID = nodeID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }
}
