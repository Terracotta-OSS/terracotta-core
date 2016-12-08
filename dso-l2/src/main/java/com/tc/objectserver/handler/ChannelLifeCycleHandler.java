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

import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.async.api.StageManager;
import com.tc.async.impl.InBandMoveToNextSink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateManager;
import com.tc.util.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.entity.ClientEntityStateManager;
import java.util.List;
import java.util.stream.Collectors;


public class ChannelLifeCycleHandler implements DSOChannelManagerEventListener {
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final StateManager                coordinator;
  private final ClientEntityStateManager      clientEvents;
  private final ManagementTopologyEventCollector collector;

  private static final TCLogger         logger = TCLogging.getLogger(ChannelLifeCycleHandler.class);
  private final Sink<HydrateContext> hydrateSink;
  private final Sink<VoltronEntityMessage> processTransactionSink;
  private final Sink<ReplicationMessage> replicatedTransactionSink;
  private final Sink<Runnable> requestProcessorSink;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager, StageManager stageManager, 
                                 DSOChannelManager channelManager, ClientEntityStateManager chain, StateManager coordinator, ManagementTopologyEventCollector collector) {
    this.commsManager = commsManager;
    this.channelMgr = channelManager;
    this.coordinator = coordinator;
    this.clientEvents = chain;
    this.collector = collector;
    hydrateSink = stageManager.getStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, HydrateContext.class).getSink();
    processTransactionSink = stageManager.getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class).getSink();
    replicatedTransactionSink = stageManager.getStage(ServerConfigurationContext.PASSIVE_REPLICATION_STAGE, ReplicationMessage.class).getSink();
    requestProcessorSink = stageManager.getStage(ServerConfigurationContext.REQUEST_PROCESSOR_STAGE, Runnable.class).getSink();
  }

  /**
   * These methods are called for both L1 and L2 when this server is in active mode. For L1s we go thru the cleanup of
   * sinks (@see below), for L2s group events will trigger this eventually.
   */
  private void nodeDisconnected(NodeID nodeID, ProductID productId, boolean wasActive) {
    // We want to track this if it is an L1 (ClientID) disconnecting.
    if (NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      ClientID clientID = (ClientID) nodeID;
      // Broadcast locally (chain) and remotely this message if active
      if (coordinator.isActiveCoordinator()) {
        broadcastClientClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, clientID, productId);
        // by here, all the messages from the client have been processed, so if any 
        // entities are still referenced, they need to be released with this synthetic 
        // message.  There is an ordering hack here to let the collector know what releases
        // are coming so the collector can present releases before final disconnect
        List<VoltronEntityMessage> msg = clientEvents.clientDisconnected(clientID);
        if (!msg.isEmpty()) {
          collector.expectedReleases(clientID, msg.stream().map(m->m.getEntityDescriptor()).collect(Collectors.toList()));
          msg.forEach(m->processTransactionSink.addSingleThreaded(m));
        }
        if (wasActive) {
          notifyTopoCollectorDisconnected(clientID);
        }
      }
    }
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
    }
  }
  
  private void notifyTopoCollectorDisconnected(ClientID client) {
    collector.clientDidDisconnect(client);
  }
  
  private void notifyTopoCollectorConnected(ClientID client) {
    try {
      collector.clientDidConnect(this.channelMgr.getActiveChannel(client), client);
    } catch (NoSuchChannelException nochan) {
      logger.warn("client channel is not available for " + client);
    }
  }

  private void nodeConnected(NodeID nodeID, ProductID productId) {
    // We want to track this if it is an L1 (ClientID) connecting.
    if (NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      ClientID clientID = (ClientID) nodeID;
      // Broadcast locally (chain) and remotely this message if active
      if (coordinator.isActiveCoordinator()) {
        broadcastClientClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, clientID, productId);
        notifyTopoCollectorConnected(clientID);
      }
    }
  }

  private void broadcastClientClusterMembershipMessage(int eventType, ClientID clientID, ProductID productId) {
    // Only broadcast when the current server is the active coordinator.
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

  @Override
  public void channelCreated(MessageChannel channel) {
    ClientID clientID = new ClientID(channel.getChannelID().toLong());
    clientCreated(clientID, channel.getProductId());
  }
  
  public void clientCreated(ClientID client, ProductID product) {
    nodeConnected(client, product);
  }
  
  public void clientDropped(ClientID clientID, ProductID product, boolean wasActive) {
    // Note that the remote node ID always refers to a client, in this path.
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. This goo is for that.
    NodeID inBandSchedulerKey = clientID;
    SpecializedEventContext sec = new SpecializedEventContext() {
      @Override
      public void execute() throws EventHandlerException {
        requestProcessorSink.addMultiThreaded(new FlushThenDisconnect(clientID, product, wasActive));
      }

      @Override
      public Object getSchedulingKey() {
        return 0;
      }

      @Override
      public boolean flush() {
        return true;
      }
    };
    
    InBandMoveToNextSink context3 = new InBandMoveToNextSink(null, sec, coordinator.isActiveCoordinator() ? processTransactionSink : replicatedTransactionSink, inBandSchedulerKey, false);  // threaded on client nodeid so no need to flush
    hydrateSink.addSpecialized(context3);
  }

  @Override
  public void channelRemoved(MessageChannel channel, boolean wasActive) {
    // Note that the remote node ID always refers to a client, in this path.
    ClientID clientID = (ClientID) channel.getRemoteNodeID();
    ProductID product = channel.getProductId();
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. 
    // this will only get fired on the active as this is a client removal.
    // the chain is hydrate stage -> process transaction handler -> request processor (flushed) -> deliver event to 
    // disconnect node.  This is done so that all messages issued by the client have fully run their course 
    // before an attempt is made to remove references.
    clientDropped(clientID, product, wasActive);
  }  
  
  private class FlushThenDisconnect implements MultiThreadedEventContext, Runnable {
    
    private final ClientID clientID;
    private final ProductID product;
    private final boolean wasActive;

    public FlushThenDisconnect(ClientID client, ProductID product, boolean wasActive) {
      this.clientID = client;
      this.product = product;
      this.wasActive = wasActive;
    }

    @Override
    public Object getSchedulingKey() {
      return 0;
    }

    @Override
    public boolean flush() {
      return true;
    }

    @Override
    public void run() {
      nodeDisconnected(clientID, product, wasActive);
    }
  }
}
