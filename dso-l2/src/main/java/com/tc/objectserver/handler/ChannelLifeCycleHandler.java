/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.InBandMoveToNextSink;
import com.tc.config.HaConfig;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.TransactionBatchManager;

public class ChannelLifeCycleHandler extends AbstractEventHandler implements DSOChannelManagerEventListener {
  private final TransactionBatchManager transactionBatchManager;
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final HaConfig                haConfig;

  private TCLogger                      logger;
  private Sink                          channelSink;
  private Sink                          hydrateSink;
  private Sink                          processTransactionSink;

  public ChannelLifeCycleHandler(final CommunicationsManager commsManager,
                                 final TransactionBatchManager transactionBatchManager,
                                 final DSOChannelManager channelManager, final HaConfig haConfig) {
    this.commsManager = commsManager;
    this.transactionBatchManager = transactionBatchManager;
    this.channelMgr = channelManager;
    this.haConfig = haConfig;
  }

  @Override
  public void handleEvent(final EventContext context) {
    NodeStateEventContext event = (NodeStateEventContext) context;

    switch (event.getType()) {
      case NodeStateEventContext.CREATE: {
        nodeConnected(event.getNodeID());
        break;
      }
      case NodeStateEventContext.REMOVE: {
        nodeDisconnected(event.getNodeID());
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
  private void nodeDisconnected(final NodeID nodeID) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, nodeID);
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
      transactionBatchManager.shutdownNode(nodeID);
    }
  }

  private void nodeConnected(final NodeID nodeID) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, nodeID);
    transactionBatchManager.nodeConnected(nodeID);
  }

  private void broadcastClusterMembershipMessage(final int eventType, final NodeID nodeID) {
    // only broadcast cluster membership messages for L1 nodes when the current server is the active coordinator
    if (haConfig.isActiveCoordinatorGroup() && NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      MessageChannel[] channels = channelMgr.getActiveChannels();
      for (MessageChannel channel : channels) {
        if (!channelMgr.getClientIDFor(channel.getChannelID()).equals(nodeID)) {
          ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
              .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
          cmm.initialize(eventType, nodeID, channels);
          cmm.send();
        }
      }
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
    channelSink = scc.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink();
    hydrateSink = scc.getStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK).getSink();
    processTransactionSink = scc.getStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE).getSink();
  }

  public void channelCreated(final MessageChannel channel) {
    channelSink.add(new NodeStateEventContext(NodeStateEventContext.CREATE, new ClientID(channel.getChannelID()
        .toLong())));
  }

  public void channelRemoved(final MessageChannel channel) {
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. This goo is for that.
    final NodeStateEventContext disconnectEvent = new NodeStateEventContext(NodeStateEventContext.REMOVE,
                                                                            channel.getRemoteNodeID());
    InBandMoveToNextSink context1 = new InBandMoveToNextSink(disconnectEvent, channelSink, channel.getRemoteNodeID());
    InBandMoveToNextSink context2 = new InBandMoveToNextSink(context1, processTransactionSink, channel.getRemoteNodeID());
    hydrateSink.add(context2);
  }

}
