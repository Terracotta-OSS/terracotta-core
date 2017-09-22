package com.tc.objectserver.handler;

import com.tc.net.NodeID;

public class FlushPassiveMessageBatch extends ReplicationSenderMessage {
  public FlushPassiveMessageBatch(NodeID nodeID) {
    super(nodeID);
  }
}
