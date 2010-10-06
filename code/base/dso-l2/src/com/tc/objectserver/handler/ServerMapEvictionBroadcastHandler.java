/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ServerMapEvictionBroadcastMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.context.ServerMapEvictionBroadcastContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.sampled.SampledCounter;

public class ServerMapEvictionBroadcastHandler extends AbstractEventHandler implements EventHandler {

  private DSOChannelManager    channelManager;
  private final SampledCounter broadcastCounter;
  private ClientStateManager   clientStateManager;

  public ServerMapEvictionBroadcastHandler(SampledCounter broadcastCounter) {
    this.broadcastCounter = broadcastCounter;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (!(context instanceof ServerMapEvictionBroadcastContext)) {
      //
      throw new AssertionError(ServerMapEvictionBroadcastHandler.class.getName() + " can only handle types of "
                               + ServerMapEvictionBroadcastContext.class.getName() + ". Unknown context: "
                               + context.getClass().getName());
    }
    final ServerMapEvictionBroadcastContext broadcastContext = (ServerMapEvictionBroadcastContext) context;
    final MessageChannel[] channels = channelManager.getActiveChannels();
    for (MessageChannel channel : channels) {
      if (channel.isClosed()) {
        continue;
      }
      if (clientStateManager.hasReference(channel.getLocalNodeID(), broadcastContext.getMapOid())) {
        final ServerMapEvictionBroadcastMessage broadcastMessage = (ServerMapEvictionBroadcastMessage) channel
            .createMessage(TCMessageType.EVICTION_SERVER_MAP_BROADCAST_MESSAGE);
        broadcastMessage.initializeEvictionBroadcastMessage(broadcastContext.getMapOid(), broadcastContext
            .getEvictedKeys());
        broadcastMessage.send();
        broadcastCounter.increment();
      }
    }
  }

  @Override
  protected void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    clientStateManager = scc.getClientStateManager();
  }
}
