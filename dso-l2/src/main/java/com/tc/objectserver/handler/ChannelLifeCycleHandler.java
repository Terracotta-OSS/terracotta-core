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

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.entity.VoltronEntityMessage;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.entity.ClientDisconnectMessage;
import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.util.ProductID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ChannelLifeCycleHandler implements DSOChannelManagerEventListener {
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final StateManager                coordinator;
  private final ClientEntityStateManager      clientEvents;
  private final ManagementTopologyEventCollector collector;
  
  private final Set<ClientID>  knownClients = new HashSet<>();

  private static final Logger logger = LoggerFactory.getLogger(ChannelLifeCycleHandler.class);
  private final Sink<VoltronEntityMessage> voltronSink;
  private final Sink<VoltronEntityMessage> pth;
  private final Sink<Runnable> requestProcessorSink;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager,
                                 StageManager stageManager,
                                 DSOChannelManager channelManager,
                                 ClientEntityStateManager chain,
                                 StateManager coordinator,
                                 Sink<VoltronEntityMessage>  voltron, 
                                 Sink<VoltronEntityMessage>  pth, 
                                 ManagementTopologyEventCollector collector) {
    this.commsManager = commsManager;
    this.channelMgr = channelManager;
    this.coordinator = coordinator;
    this.clientEvents = chain;
    this.collector = collector;
    this.voltronSink = voltron;
    this.pth = pth;
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
        List<FetchID> msg = clientEvents.clientDisconnected(clientID);
        collector.expectedDisconnects(clientID, msg);
        msg.forEach(m->voltronSink.addSingleThreaded(new ClientDisconnectMessage(clientID,EntityDescriptor.createDescriptorForInvoke(m, ClientInstanceID.NULL_ID))));
        if (wasActive) {
          notifyClientRemoved(clientID);
        }
      }
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
      // Broadcast locally (chain) and remotely this message if active
      if (coordinator.isActiveCoordinator()) {
        broadcastClientClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, clientID, productId);
      }
    }
  }

  private void broadcastClientClusterMembershipMessage(int eventType, ClientID clientID, ProductID productId) {
    // Only broadcast when the current server is the active coordinator.
    // Do not broadcast for internal clients
    if (!productId.isInternal()) {
      MessageChannel[] channels = channelMgr.getActiveChannels();
      for (MessageChannel channel : channels) {
        // only broadcast to clients that are not internal
        if (!channelMgr.getClientIDFor(channel.getChannelID()).equals(clientID) && !channel.getProductId().isInternal()) {
          ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
              .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
          cmm.initialize(eventType, clientID, productId);
          cmm.send();
        }
      }
    }
  }
/**
 * channel created is strangely connected.  When a new channel is created, it can be 
 * either a brand new client connected or just a client reconnecting (failover).
 * All the accounting for the client should all ready be there for failover created channels 
 * 
 * @param channel
 */
  @Override
  public void channelCreated(MessageChannel channel) {
    ClientID clientID = (ClientID)channel.getRemoteNodeID();
 //  brand new member, broadcast the change if active
    clientCreated(clientID, channel.getProductId());
 //  client is connecting to the active
    if (coordinator.isActiveCoordinator()) {
      notifyClientAdded(channel, clientID);
    }
  }
  
  private void notifyClientAdded(MessageChannel channel, ClientID clientID) {
    synchronized (knownClients) {
      collector.clientDidConnect(channel, clientID);
      knownClients.add(clientID);
    }
  }
  
  private void notifyClientRemoved(ClientID clientID) {
    synchronized (knownClients) {
      if (knownClients.contains(clientID)) {
        collector.clientDidDisconnect(clientID);
        knownClients.remove(clientID);
      }
    }
  }
  
  public void clientCreated(ClientID client, ProductID product) {
    nodeConnected(client, product);
  }
  
  public void clientDropped(ClientID clientID, ProductID product, boolean wasActive) {
    if (coordinator.isActiveCoordinator()) {
      nodeDisconnected(clientID, product, wasActive);
    }
  }
/**
 * Again, channel disconnection is oddly connected.  A channel disconnect can come as
 * a result of a reconnect timeout so not all channel removals represent a physical 
 * connection of a client to this machine.  Only report to the topo collector if it 
 * was physically connected
 * @param channel
 * @param wasActive 
 */
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
}
