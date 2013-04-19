package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.msg.RegisterInterestListenerMessage;
import com.tc.object.msg.UnregisterInterestListenerMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.InClusterInterestNotifier;
import com.tc.util.Assert;

/**
 * A stage handler for {@link ServerConfigurationContext#REGISTER_INTEREST_LISTENER_STAGE}.
 *
 * @author Eugene Shelestovich
 */
public class RegisterInterestListenerHandler extends AbstractEventHandler {

  private static final TCLogger LOG = TCLogging.getLogger(RegisterInterestListenerHandler.class);

  private final InClusterInterestNotifier interestNotifier;

  public RegisterInterestListenerHandler(final InClusterInterestNotifier interestNotifier) {
    this.interestNotifier = interestNotifier;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final NodeID nodeId = ((TCMessage)context).getSourceNodeID();
    if (nodeId.getNodeType() == NodeID.CLIENT_NODE_TYPE) {
      final ClientID clientId = (ClientID)nodeId;
      if (context instanceof RegisterInterestListenerMessage) {
        final RegisterInterestListenerMessage msg = (RegisterInterestListenerMessage)context;
        interestNotifier.subscribe(clientId, msg.getDestinationName(), msg.getInterestTypes());
        LOG.info("A new interest listener registration message from client [" + nodeId + "] has been received: " + context);
        LOG.info("Destination: " + msg.getDestinationName() + ", interest: " + msg.getInterestTypes());
      } else if (context instanceof UnregisterInterestListenerMessage) {
        final UnregisterInterestListenerMessage msg = (UnregisterInterestListenerMessage)context;
        interestNotifier.unsubscribe(clientId);
      } else {
        Assert.fail("Unknown event type " + context.getClass().getName());
      }
    }
  }

}
