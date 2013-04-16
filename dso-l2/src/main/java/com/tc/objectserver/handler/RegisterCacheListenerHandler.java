package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.msg.RegisterCacheListenerMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.InClusterInterestBroadcaster;
import com.tc.util.Assert;

/**
 * A stage handler for {@link ServerConfigurationContext#REGISTER_CACHE_LISTENER_STAGE}.
 *
 * @author Eugene Shelestovich
 */
public class RegisterCacheListenerHandler extends AbstractEventHandler {

  private final InClusterInterestBroadcaster evictionBroadcastManager;

  public RegisterCacheListenerHandler(final InClusterInterestBroadcaster evictionBroadcastManager) {
    this.evictionBroadcastManager = evictionBroadcastManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof RegisterCacheListenerMessage) {
      final RegisterCacheListenerMessage msg = (RegisterCacheListenerMessage)context;
      final NodeID clientId = msg.getSourceNodeID();
      if (NodeID.CLIENT_NODE_TYPE == clientId.getNodeType()) {
        evictionBroadcastManager.addClient((ClientID)clientId);
      }
    } else {
      Assert.fail("Unknown event type " + context.getClass().getName());
    }
  }

  @Override
  public void initialize(final ConfigurationContext ctxt) {
    super.initialize(ctxt);
    final ServerConfigurationContext scc = (ServerConfigurationContext)ctxt;
    // init
  }
}
