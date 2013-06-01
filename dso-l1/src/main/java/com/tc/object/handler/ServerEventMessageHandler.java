package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.server.ServerEvent;

/**
 * Handles batched event messages coming from server.
 *
 * @author Eugene Shelestovich
 */
public class ServerEventMessageHandler extends AbstractEventHandler {

  private final ServerEventListenerManager manager;

  public ServerEventMessageHandler(final ServerEventListenerManager manager) {
    this.manager = manager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerEventBatchMessage) {
      final ServerEventBatchMessage message = (ServerEventBatchMessage)context;
      final NodeID remoteNode = message.getChannel().getRemoteNodeID();
      // unfold the batch
      for (ServerEvent event : message.getEvents()) {
        manager.dispatch(event, remoteNode);
      }
    } else {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
  }
}
