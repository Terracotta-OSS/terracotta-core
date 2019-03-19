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
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.GuardianContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.entity.ClientDisconnectMessage;
import com.tc.objectserver.entity.ClientEntityStateManager;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.util.ProductID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;


public class ClientChannelLifeCycleHandler implements ChannelManagerEventListener {
  private final CommunicationsManager   commsManager;
  private final DSOChannelManager       channelMgr;
  private final ClientEntityStateManager      clientEvents;
  private final ProcessTransactionHandler   pth;
  private final ManagementTopologyEventCollector collector;
  private boolean guardian = false;
  
  private final Set<ClientID>  knownClients = new HashSet<>();

  private static final Logger logger = LoggerFactory.getLogger(ClientChannelLifeCycleHandler.class);
  private final Sink<VoltronEntityMessage> voltronSink;

  public ClientChannelLifeCycleHandler(CommunicationsManager commsManager,
                                 StageManager stageManager,
                                 DSOChannelManager channelManager,
                                 ClientEntityStateManager chain,
                                 ProcessTransactionHandler handler,
                                 ManagementTopologyEventCollector collector) {
    this.commsManager = commsManager;
    this.channelMgr = channelManager;
    this.clientEvents = chain;
    this.collector = collector;
    this.pth = handler;
    this.voltronSink = stageManager.getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class).getSink();
  }

  /**
   * These methods are called for both L1 and L2 when this server is in active mode. For L1s we go thru the cleanup of
   * sinks (@see below), for L2s group events will trigger this eventually.
   */
  private void nodeDisconnected(NodeID nodeID, ProductID productId) {
    // We want to track this if it is an L1 (ClientID) disconnecting.
    if (NodeID.CLIENT_NODE_TYPE == nodeID.getNodeType()) {
      ClientID clientID = (ClientID) nodeID;
      voltronSink.addToSink(createDisconnectMessage(clientID));
    }

    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + nodeID + " while shutting down.");
    } else {
      logger.info(": Received transport disconnect.  Shutting down client " + nodeID);
    }
  }
  
  private VoltronEntityMessage createDisconnectMessage(ClientID clientID) {
    return new ClientDisconnectMessage(clientID,EntityDescriptor.createDescriptorForInvoke(PlatformEntity.PLATFORM_FETCH_ID, ClientInstanceID.NULL_ID), ()-> {
//  now the pth is flushed, check that there are no pending removes in the managed entities
      if (pth.removeClient(clientID)) {
    // no fetches, safe to remove the client
          notifyEnitiesOfDisconnect(clientID);
        } else {
    // there are fetches in the managed entity, not safe to remove the client, recursion
          voltronSink.addToSink(createDisconnectMessage(clientID));
        }
      });
  }
  
  private void notifyEnitiesOfDisconnect(ClientID clientID) {
    List<FetchID> msg = clientEvents.clientDisconnected(clientID);
    collector.expectedDisconnects(clientID, msg);
    if (msg.isEmpty()) {
      notifyClientRemoved(clientID);
    } else {
      CountDownLatch latch = new CountDownLatch(msg.size());
      msg.forEach(m->voltronSink.addToSink(new ClientDisconnectMessage(clientID,EntityDescriptor.createDescriptorForInvoke(m, ClientInstanceID.NULL_ID), ()->{
        latch.countDown();
        if (latch.getCount() == 0) {
          notifyClientRemoved(clientID);
        }
      })));
    }
  }

  private void nodeConnected(NodeID nodeID, ProductID productId) {

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
    if (this.channelMgr.isActiveID(clientID)) {
   //  brand new member, broadcast the change if active
      nodeConnected(clientID, channel.getProductID());
   //  client is connecting to the active
      notifyClientAdded(channel, clientID);
    } else {
      //  well, either the client already died and is not active or this is an internal 
      //  connection
    }
    if (this.guardian) {
      GuardianContext.channelCreated(channel);
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
        if (this.guardian) {
          GuardianContext.clientRemoved(clientID);
        }
        knownClients.remove(clientID);
      }
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
  private void channelRemoved(MessageChannel channel, boolean wasActive) {
    // Note that the remote node ID always refers to a client, in this path.
    ClientID clientID = (ClientID) channel.getRemoteNodeID();
    ProductID product = channel.getProductID();
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. 
    // this will only get fired on the active as this is a client removal.
    // the chain is hydrate stage -> process transaction handler -> request processor (flushed) -> deliver event to 
    // disconnect node.  This is done so that all messages issued by the client have fully run their course 
    // before an attempt is made to remove references.
    if (wasActive) {
      nodeDisconnected(clientID, product);
    } else if (this.guardian) {
      GuardianContext.channelRemoved(channel);
    }
  }  

  @Override
  public void channelRemoved(MessageChannel channel) {
    channelRemoved(channel, this.channelMgr.isActiveID(channel.getRemoteNodeID()));
  }
  
  public void activateGuardian() {
    this.guardian = true;
  }
}
