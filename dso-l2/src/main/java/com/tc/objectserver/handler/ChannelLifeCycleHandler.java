/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.InBandMoveToNextSink;
import com.tc.config.HaConfig;
import com.tc.entity.VoltronEntityMessage;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class ChannelLifeCycleHandler extends AbstractEventHandler<NodeStateEventContext> implements DSOChannelManagerEventListener {
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final HaConfig                haConfig;

  private TCLogger                      logger;
  private Sink<NodeStateEventContext> channelSink;
  private Sink<HydrateContext> hydrateSink;
  private Sink<VoltronEntityMessage> processTransactionSink;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager,
                                 DSOChannelManager channelManager, HaConfig haConfig) {
    this.commsManager = commsManager;
    this.channelMgr = channelManager;
    this.haConfig = haConfig;
  }

  @Override
  public void handleEvent(NodeStateEventContext event) {
    switch (event.getType()) {
      case NodeStateEventContext.CREATE: {
        nodeConnected(event.getNodeID(), event.getProductId());
        break;
      }
      case NodeStateEventContext.REMOVE: {
        nodeDisconnected(event.getNodeID(), event.getProductId());
        break;
      }
      default: {
        throw new AssertionError("unknown event: " + event.getType());
      }
    }
  }

  /**
   * These methods are called for both L1 and L2 when this server is in active mode. For L1s we go thru the cleanup of
   * sinks (@see below), for L2s group events will trigger this eventually.
   */
  private void nodeDisconnected(NodeID nodeID, ProductID productId) {
    // We want to track this if it is an L1 (ClientID) disconnecting.
    if (NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      ClientID clientID = (ClientID) nodeID;
      // Broadcast this message.
      broadcastClientClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, clientID, productId);
    }
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
    }
  }

  private void nodeConnected(NodeID nodeID, ProductID productId) {
    // We want to track this if it is an L1 (ClientID) connecting.
    if (NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      ClientID clientID = (ClientID) nodeID;
      // Broadcast this message.
      broadcastClientClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, clientID, productId);
    }
  }

  private void broadcastClientClusterMembershipMessage(int eventType, ClientID clientID, ProductID productId) {
    // Only broadcast when the current server is the active coordinator.
    if (haConfig.isActiveCoordinatorGroup()) {
      MessageChannel[] channels = channelMgr.getActiveChannels();
      for (MessageChannel channel : channels) {
        if (!channelMgr.getClientIDFor(channel.getChannelID()).equals(clientID)) {
          ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
              .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
          cmm.initialize(eventType, clientID, productId);
          cmm.send();
        }
      }
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
    channelSink = scc.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, NodeStateEventContext.class).getSink();
    hydrateSink = scc.getStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, HydrateContext.class).getSink();
    processTransactionSink = scc.getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class).getSink();
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    ClientID clientID = new ClientID(channel.getChannelID().toLong());
    channelSink.addMultiThreaded(new NodeStateEventContext(NodeStateEventContext.CREATE, clientID, channel.getProductId()));
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. This goo is for that.
    final NodeStateEventContext disconnectEvent = new NodeStateEventContext(NodeStateEventContext.REMOVE,
                                                                            channel.getRemoteNodeID(), channel.getProductId());
    NodeID inBandSchedulerKey = channel.getRemoteNodeID();
    InBandMoveToNextSink<NodeStateEventContext> context1 = new InBandMoveToNextSink<>(disconnectEvent, null, channelSink, inBandSchedulerKey, false); // single threaded so no need to flush
    InBandMoveToNextSink<VoltronEntityMessage> context2 = new InBandMoveToNextSink<>(null, context1, processTransactionSink, inBandSchedulerKey, false);  // threaded on client nodeid so no need to flush
    hydrateSink.addSpecialized(context2);
  }

}
