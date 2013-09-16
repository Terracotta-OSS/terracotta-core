package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.server.ServerEvent;

/**
 * Handles batches of events coming from server and multiplexes them.
 *
 * @author Eugene Shelestovich
 */
public class ServerEventMessageHandler extends AbstractEventHandler {

  private final Sink deliverySink;

  public ServerEventMessageHandler(final Sink deliverySink) {
    this.deliverySink = deliverySink;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerEventBatchMessage) {
      final ServerEventBatchMessage message = (ServerEventBatchMessage) context;
      final NodeID remoteNode = message.getChannel().getRemoteNodeID();
      // unfold the batch and multiplex messages to different queues based on the event key
      for (final ServerEvent event : message.getEvents()) {
        deliverySink.add(new ServerEventDeliveryContext(event, remoteNode));
      }
    } else {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
  }
}
