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
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerMapEvictionBroadcastHandler extends AbstractEventHandler implements EventHandler {

  private static final int     EVICTION_BROADCAST_MAX_KEYS = TCPropertiesImpl
                                                               .getProperties()
                                                               .getInt(TCPropertiesConsts.L2_SERVERMAP_EVICTION_BROADCAST_MAXKEYS,
                                                                       10000);

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
    int clientIndex = 0;
    for (MessageChannel channel : channels) {
      if (channel.isClosed()) {
        continue;
      }
      if (clientStateManager.hasReference(channel.getRemoteNodeID(), broadcastContext.getMapOid())) {
        for (Set keysBatch : getEvictedKeysInBatches(broadcastContext.getEvictedKeys(), EVICTION_BROADCAST_MAX_KEYS)) {
          final ServerMapEvictionBroadcastMessage broadcastMessage = (ServerMapEvictionBroadcastMessage) channel
              .createMessage(TCMessageType.EVICTION_SERVER_MAP_BROADCAST_MESSAGE);
          broadcastMessage.initializeEvictionBroadcastMessage(broadcastContext.getMapOid(), keysBatch, clientIndex++);
          broadcastMessage.send();
          broadcastCounter.increment();
        }
      }
    }
  }

  static List<Set> getEvictedKeysInBatches(Set evictedKeys, int maxBatchSize) {
    int size = evictedKeys.size();
    if (size <= maxBatchSize) { return Collections.singletonList(evictedKeys); }
    List<Set> rv = new ArrayList();
    Set currentBatch = new HashSet((int) (maxBatchSize * 1.5));
    for (Object obj : evictedKeys) {
      if (currentBatch.size() >= maxBatchSize) {
        rv.add(currentBatch);
        currentBatch = new HashSet((int) (maxBatchSize * 1.5));
      }
      currentBatch.add(obj);
    }
    if (!currentBatch.isEmpty()) {
      rv.add(currentBatch);
    }
    return rv;
  }

  @Override
  protected void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    clientStateManager = scc.getClientStateManager();
  }
}
