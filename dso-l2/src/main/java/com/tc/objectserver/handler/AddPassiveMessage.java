package com.tc.objectserver.handler;

import com.tc.net.NodeID;

public class AddPassiveMessage extends ReplicationSenderMessage {

  private final Runnable completionHandler;

  public AddPassiveMessage(NodeID passive, Runnable completionHandler) {
    super(passive);
    this.completionHandler = completionHandler;
  }

  public Runnable getCompletionHandler() {
    return completionHandler;
  }
}
