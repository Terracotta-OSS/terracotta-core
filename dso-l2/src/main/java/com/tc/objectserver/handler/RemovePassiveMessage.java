package com.tc.objectserver.handler;

import com.tc.net.NodeID;

public class RemovePassiveMessage extends ReplicationSenderMessage {
  private final Runnable completionHandler;

  public Runnable getCompletionHandler() {
    return completionHandler;
  }

  public RemovePassiveMessage(NodeID nodeID, Runnable completionHandler) {
    super(nodeID);
    this.completionHandler = completionHandler;


  }
}
