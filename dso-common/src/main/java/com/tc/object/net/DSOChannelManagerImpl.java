/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.net;

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
import com.tc.util.concurrent.CopyOnWriteSequentialMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world and to provide delayed visibility of
 * clients and hides the channel to client ID mapping from the rest of the world.
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean {
  private static final TCLogger      logger         = TCLogging.getLogger(DSOChannelManager.class);

  private final CopyOnWriteSequentialMap<NodeID, MessageChannel> activeChannels = new CopyOnWriteSequentialMap<NodeID, MessageChannel>(
                                                                              new CopyOnWriteSequentialMap.TypedArrayFactory() {
                                                                                                                 @Override
                                                                                                                 public MessageChannel[] createTypedArray(final int size) {
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

  @Override
  public MessageChannel getActiveChannel(final NodeID id) throws NoSuchChannelException {
    final MessageChannel rv = activeChannels.get(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  @Override
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

  @Override
  public MessageChannel[] getActiveChannels() {
    return activeChannels.valuesToArray();
  }

  @Override
  public boolean isActiveID(final NodeID nodeID) {
    return activeChannels.containsKey(nodeID);
  }

  @Override
  public String getChannelAddress(final NodeID nid) {
    try {
      MessageChannel channel = getActiveChannel(nid);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  @Override
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

  @Override
  public TCConnection[] getAllActiveClientConnections() {
    return connectionManager.getAllActiveConnections();
  }

  @Override
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

  private Set<ClientID> getAllActiveClientIDs() {
    Set<ClientID> clientIDs = new HashSet<ClientID>();
      for (Object element : activeChannels.keySet()) {
        ClientID cid = (ClientID) element;
        clientIDs.add(cid);
      }
    return clientIDs;
  }

  @Override
  public void makeChannelActiveNoAck(final MessageChannel channel) {
    activeChannels.put(getClientIDFor(channel.getChannelID()), channel);
  }

  @Override
  public void addEventListener(final DSOChannelManagerEventListener listener) {
    if (listener == null) { throw new NullPointerException("listener cannot be be null"); }
    eventListeners.add(listener);
  }

  @Override
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

    @Override
    public void channelCreated(final MessageChannel channel) {
      // nothing
    }

    @Override
    public void channelRemoved(final MessageChannel channel) {
      activeChannels.remove(getClientIDFor(channel.getChannelID()));
      fireChannelRemovedEvent(channel);
    }

  }

  @Override
  public ClientID getClientIDFor(final ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

}
