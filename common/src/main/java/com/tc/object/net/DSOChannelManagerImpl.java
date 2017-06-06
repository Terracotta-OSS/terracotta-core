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
package com.tc.object.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;
import com.tc.util.concurrent.CopyOnWriteSequentialMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world and to provide delayed visibility of
 * clients and hides the channel to client ID mapping from the rest of the world.
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean {
  private static final Logger logger = LoggerFactory.getLogger(DSOChannelManager.class);

  private final CopyOnWriteSequentialMap<NodeID, MessageChannel> activeChannels = new CopyOnWriteSequentialMap<NodeID, MessageChannel>(
                                                                                      new CopyOnWriteSequentialMap.TypedArrayFactory() {
                                                                                        @SuppressWarnings("unchecked")
                                                                                        @Override
                                                                                        public MessageChannel[] createTypedArray(int size) {
                                                                                          return new MessageChannel[size];
                                                                                        }
                                                                                      });
  
  private final List<DSOChannelManagerEventListener> eventListeners = new CopyOnWriteArrayList<DSOChannelManagerEventListener>();

  private final ChannelManager       genericChannelManager;
  private final TCConnectionManager  connectionManager;
  private final String               serverVersion;

  public DSOChannelManagerImpl(ChannelManager genericChannelManager,
                               TCConnectionManager connectionManager, String serverVersion) {
    this.genericChannelManager = genericChannelManager;
    this.genericChannelManager.addEventListener(new GenericChannelEventListener());
    this.serverVersion = serverVersion;
    this.connectionManager = connectionManager;
  }

  @Override
  public MessageChannel getActiveChannel(NodeID id) throws NoSuchChannelException {
    final MessageChannel rv = activeChannels.get(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  @Override
  public void closeAll(Collection<? extends NodeID> clientIDs) {
    for (NodeID nid : clientIDs) {
      // we might get passed a ServerID here for server generated transactions
      if (nid instanceof ClientID) {
        ClientID id = (ClientID) nid;

        MessageChannel channel = genericChannelManager.getChannel(new ChannelID(id.toLong()));
        if (channel != null) {
          channel.close();
        }
      } else {
        logger.info("Ignoring close for " + nid);
      }
    }
  }

  @Override
  public MessageChannel[] getActiveChannels() {
    return activeChannels.valuesToArray();
  }

  @Override
  public boolean isActiveID(NodeID nodeID) {
    return activeChannels.containsKey(nodeID);
  }

  @Override
  public String getChannelAddress(NodeID nid) {
    try {
      MessageChannel channel = getActiveChannel(nid);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  private ClientHandshakeRefusedMessage newClientHandshakeRefusedMessage(ClientID clientID)
      throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(new ChannelID(clientID.toLong()));
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeRefusedMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE);
  }

  private ClientHandshakeAckMessage newClientHandshakeAckMessage(ClientID clientID) throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(new ChannelID(clientID.toLong()));
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeAckMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
  }

  @Override
  public TCConnection[] getAllActiveClientConnections() {
    return connectionManager.getAllActiveConnections();
  }

  @Override
  public void makeChannelActive(ClientID clientID) {
    try {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      MessageChannel channel = ackMsg.getChannel();
      synchronized (activeChannels) {
        activeChannels.put(clientID, channel);
        ackMsg.initialize(getAllActiveClientIDs(), clientID, serverVersion);
        if (!ackMsg.send()) {
          logger.warn("Not sending handshake message to disconnected client: " + clientID);
        }
      }
      fireChannelCreatedEvent(channel);
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake message to disconnected client: " + clientID);
    }
  }

  @Override
  public void makeChannelRefuse(ClientID clientID, String message) {
    try {
      ClientHandshakeRefusedMessage handshakeRefuseMsg = newClientHandshakeRefusedMessage(clientID);
      synchronized (activeChannels) {
        handshakeRefuseMsg.initialize(message);
        if (!handshakeRefuseMsg.send()) {
          logger.warn("Not sending handshake rejected message to disconnected client: " + clientID);
        }
      }
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake rejected message to disconnected client: " + clientID);
    }
  }

  private Set<? extends NodeID> getAllActiveClientIDs() {
    Set<NodeID> clientIDs = new HashSet<NodeID>();
      for (NodeID cid : activeChannels.keySet()) {
        clientIDs.add(cid);
      }
    return clientIDs;
  }

  @Override
  public void makeChannelActiveNoAck(MessageChannel channel) {
    activeChannels.put(getClientIDFor(channel.getChannelID()), channel);
  }

  @Override
  public void addEventListener(DSOChannelManagerEventListener listener) {
    if (listener == null) { throw new NullPointerException("listener cannot be be null"); }
    eventListeners.add(listener);
  }

  @Override
  public Set<NodeID> getAllClientIDs() {
    Set<ChannelID> channelIDs = genericChannelManager.getAllChannelIDs();
    Set<NodeID> clientIDs = new HashSet<NodeID>(channelIDs.size());
    for (ChannelID cid : channelIDs) {
      clientIDs.add(getClientIDFor(cid));
    }
    return clientIDs;
  }

  private void fireChannelCreatedEvent(MessageChannel channel) {
    for (DSOChannelManagerEventListener eventListener : eventListeners) {
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(MessageChannel channel) {
    boolean isActive = isActiveID(channel.getRemoteNodeID());
    for (DSOChannelManagerEventListener eventListener : eventListeners) {
      eventListener.channelRemoved(channel, isActive);
    }
  }

  private class GenericChannelEventListener implements ChannelManagerEventListener {

    @Override
    public void channelCreated(MessageChannel channel) {
      // nothing
    }

    @Override
    public void channelRemoved(MessageChannel channel) {
      fireChannelRemovedEvent(channel);
      activeChannels.remove(getClientIDFor(channel.getChannelID()));
    }

  }

  @Override
  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

}
