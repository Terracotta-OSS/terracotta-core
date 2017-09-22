package com.tc.objectserver.handler;

import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;

import java.util.function.Consumer;

public class SyncActivityMessage extends ReplicationSenderMessage {
  private final SyncReplicationActivity syncReplicationActivity;
  private final Consumer<Boolean> completionHandler;

  public SyncActivityMessage(NodeID nodeid, SyncReplicationActivity syncReplicationActivity, Consumer<Boolean> completionHandler) {
    super(nodeid);
    this.syncReplicationActivity = syncReplicationActivity;
    this.completionHandler = completionHandler;
  }

  public SyncReplicationActivity getSyncReplicationActivity() {
    return syncReplicationActivity;
  }

  public Consumer<Boolean> getCompletionHandler() {
    return completionHandler;
  }
}
