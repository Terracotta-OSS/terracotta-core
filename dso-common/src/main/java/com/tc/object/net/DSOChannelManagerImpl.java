/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world and to provide delayed visibility of
 * clients and hides the channel to client ID mapping from the rest of the world.
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean {
  private static final TCLogger      logger         = TCLogging.getLogger(DSOChannelManager.class);

  private final CopyOnWriteArrayMap  activeChannels = new CopyOnWriteArrayMap(
                                                                              new CopyOnWriteArrayMap.TypedArrayFactory() {

                                                                                public Object[] createTypedArray(final int size) {
                                                                                  return new MessageChannel[size];
                                                                                }

                                                                              });
  private final List                 eventListeners = new CopyOnWriteArrayList();

  private final GroupID              thisGroupID;
  private final ChannelManager       genericChannelManager;
  private final TCConnectionManager  connectionManager;
  private final String               serverVersion;
  private final StripeIDStateManager stripeIDStateManager;

  public DSOChannelManagerImpl(final GroupID groupID, final ChannelManager genericChannelManager,
                               final TCConnectionManager connectionManager, final String serverVersion,
                               final StripeIDStateManager stripeIDStateManager) {
    this.thisGroupID = groupID;
    this.genericChannelManager = genericChannelManager;
    this.genericChannelManager.addEventListener(new GenericChannelEventListener());
    this.serverVersion = serverVersion;
    this.connectionManager = connectionManager;
    this.stripeIDStateManager = stripeIDStateManager;
  }

  public MessageChannel getActiveChannel(final NodeID id) throws NoSuchChannelException {
    final MessageChannel rv = (MessageChannel) activeChannels.get(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  public void closeAll(final Collection clientIDs) {
    for (Iterator i = clientIDs.iterator(); i.hasNext();) {
      Object o = i.next();
      // we might get passed a ServerID here for server generated transactions
      if (o instanceof ClientID) {
        ClientID id = (ClientID) o;

        MessageChannel channel = genericChannelManager.getChannel(new ChannelID(id.toLong()));
        if (channel != null) {
          channel.close();
        }
      } else {
        logger.info("Ignoring close for " + o);
      }
    }
  }

  public MessageChannel[] getActiveChannels() {
    return (MessageChannel[]) activeChannels.valuesToArray();
  }

  public boolean isActiveID(final NodeID nodeID) {
    return activeChannels.containsKey(nodeID);
  }

  public String getChannelAddress(final NodeID nid) {
    try {
      MessageChannel channel = getActiveChannel(nid);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(final NodeID nid)
      throws NoSuchChannelException {
    return (BatchTransactionAcknowledgeMessage) getActiveChannel(nid)
        .createMessage(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE);
  }

  private ClientHandshakeRefusedMessage newClientHandshakeRefusedMessage(final ClientID clientID)
      throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(new ChannelID(clientID.toLong()));
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeRefusedMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_REFUSED_MESSAGE);
  }

  private ClientHandshakeAckMessage newClientHandshakeAckMessage(final ClientID clientID) throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(new ChannelID(clientID.toLong()));
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeAckMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
  }

  public TCConnection[] getAllActiveClientConnections() {
    return connectionManager.getAllActiveConnections();
  }

  public void makeChannelActive(final ClientID clientID, final boolean persistent) {
    try {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      MessageChannel channel = ackMsg.getChannel();
      synchronized (activeChannels) {
        activeChannels.put(clientID, channel);
        ackMsg.initialize(persistent, getAllActiveClientIDs(), clientID, serverVersion, this.thisGroupID,
                          this.stripeIDStateManager.getStripeID(this.thisGroupID),
                          this.stripeIDStateManager.getStripeIDMap(true));
        ackMsg.send();
      }
      fireChannelCreatedEvent(channel);
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake message to disconnected client: " + clientID);
    }
  }

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

  private Set<ClientID> getAllActiveClientIDs() {
    Set<ClientID> clientIDs = new HashSet<ClientID>();
    synchronized (activeChannels) {
      for (Iterator i = activeChannels.keySet().iterator(); i.hasNext();) {
        ClientID cid = (ClientID) i.next();
        clientIDs.add(cid);
      }
    }
    return clientIDs;
  }

  public void makeChannelActiveNoAck(final MessageChannel channel) {
    activeChannels.put(getClientIDFor(channel.getChannelID()), channel);
  }

  public void addEventListener(final DSOChannelManagerEventListener listener) {
    if (listener == null) { throw new NullPointerException("listener cannot be be null"); }
    eventListeners.add(listener);
  }

  public Set getAllClientIDs() {
    Set channelIDs = genericChannelManager.getAllChannelIDs();
    Set clientIDs = new HashSet(channelIDs.size());
    for (Iterator i = channelIDs.iterator(); i.hasNext();) {
      ChannelID cid = (ChannelID) i.next();
      clientIDs.add(getClientIDFor(cid));
    }
    return clientIDs;
  }

  private void fireChannelCreatedEvent(final MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      DSOChannelManagerEventListener eventListener = (DSOChannelManagerEventListener) iter.next();
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(final MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      DSOChannelManagerEventListener eventListener = (DSOChannelManagerEventListener) iter.next();
      eventListener.channelRemoved(channel);
    }
  }

  private class GenericChannelEventListener implements ChannelManagerEventListener {

    public void channelCreated(final MessageChannel channel) {
      // nothing
    }

    public void channelRemoved(final MessageChannel channel) {
      activeChannels.remove(getClientIDFor(channel.getChannelID()));
      fireChannelRemovedEvent(channel);
    }

  }

  public ClientID getClientIDFor(final ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

}
