/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, nodeID, productId);
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
    }
  }

  private void nodeConnected(NodeID nodeID, ProductID productId) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, nodeID, productId);
  }

  private void broadcastClusterMembershipMessage(int eventType, NodeID nodeID, ProductID productId) {
    // only broadcast cluster membership messages for L1 nodes when the current server is the active coordinator
    if (haConfig.isActiveCoordinatorGroup() && NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      MessageChannel[] channels = channelMgr.getActiveChannels();
      for (MessageChannel channel : channels) {
        if (!channelMgr.getClientIDFor(channel.getChannelID()).equals(nodeID)) {
          ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
              .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
          cmm.initialize(eventType, nodeID, productId);
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
