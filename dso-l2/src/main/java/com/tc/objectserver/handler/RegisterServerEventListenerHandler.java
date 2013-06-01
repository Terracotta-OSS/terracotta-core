package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.UnregisterServerEventListenerMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.event.InClusterServerEventNotifier;
import com.tc.util.Assert;

/**
 * A stage handler for {@link ServerConfigurationContext#REGISTER_SERVER_EVENT_LISTENER_STAGE}.
 *
 * @author Eugene Shelestovich
 */
public class RegisterServerEventListenerHandler extends AbstractEventHandler {

  private static final TCLogger LOG = TCLogging.getLogger(RegisterServerEventListenerHandler.class);

  private final InClusterServerEventNotifier serverEventNotifier;

  public RegisterServerEventListenerHandler(final InClusterServerEventNotifier serverEventNotifier) {
    this.serverEventNotifier = serverEventNotifier;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final NodeID nodeId = ((TCMessage)context).getSourceNodeID();
    if (nodeId.getNodeType() == NodeID.CLIENT_NODE_TYPE) {
      final ClientID clientId = (ClientID)nodeId;
      if (context instanceof RegisterServerEventListenerMessage) {
        final RegisterServerEventListenerMessage msg = (RegisterServerEventListenerMessage)context;
        serverEventNotifier.register(clientId, msg.getDestination(), msg.getEventTypes());
        LOG.debug("Server event listener registration message from client [" + nodeId + "] has been received: " + context);
        LOG.debug("Destination: " + msg.getDestination() + ", event: " + msg.getEventTypes());
      } else if (context instanceof UnregisterServerEventListenerMessage) {
        final UnregisterServerEventListenerMessage msg = (UnregisterServerEventListenerMessage)context;
        serverEventNotifier.unregister(clientId, msg.getDestination());
      } else {
        Assert.fail("Unknown event type " + context.getClass().getName());
      }
    }
  }

}
