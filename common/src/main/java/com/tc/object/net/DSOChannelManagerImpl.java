/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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
  private static final TCLogger      logger         = TCLogging.getLogger(DSOChannelManager.class);

  private final CopyOnWriteSequentialMap<NodeID, MessageChannel> activeChannels = new CopyOnWriteSequentialMap<>(
                                                                                      new CopyOnWriteSequentialMap.TypedArrayFactory() {
                                                                                        @SuppressWarnings("unchecked")
                                                                                        @Override
                                                                                        public MessageChannel[] createTypedArray(int size) {
                                                                                          return new MessageChannel[size];
                                                                                        }
                                                                                      });
  
  private final List<DSOChannelManagerEventListener> eventListeners = new CopyOnWriteArrayList<>();

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
  public void makeChannelActive(ClientID clientID, boolean persistent) {
    try {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      MessageChannel channel = ackMsg.getChannel();
      synchronized (activeChannels) {
        activeChannels.put(clientID, channel);
        ackMsg.initialize(persistent, getAllActiveClientIDs(), clientID, serverVersion);
        ackMsg.send();
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
        handshakeRefuseMsg.send();
      }
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake rejeceted message to disconnected client: " + clientID);
    }

  }

  private Set<? extends NodeID> getAllActiveClientIDs() {
    Set<NodeID> clientIDs = new HashSet<>();
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
    Set<NodeID> clientIDs = new HashSet<>(channelIDs.size());
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
    for (DSOChannelManagerEventListener eventListener : eventListeners) {
      eventListener.channelRemoved(channel);
    }
  }

  private class GenericChannelEventListener implements ChannelManagerEventListener {

    @Override
    public void channelCreated(MessageChannel channel) {
      // nothing
    }

    @Override
    public void channelRemoved(MessageChannel channel) {
      activeChannels.remove(getClientIDFor(channel.getChannelID()));
      fireChannelRemovedEvent(channel);
    }

  }

  @Override
  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

}
