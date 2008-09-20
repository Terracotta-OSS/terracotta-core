/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
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
  private static final TCLogger     logger         = TCLogging.getLogger(DSOChannelManager.class);

  private final CopyOnWriteArrayMap activeChannels = new CopyOnWriteArrayMap(
                                                                             new CopyOnWriteArrayMap.TypedArrayFactory() {

                                                                               public Object[] createTypedArray(int size) {
                                                                                 return new MessageChannel[size];
                                                                               }

                                                                             });
  private final List                eventListeners = new CopyOnWriteArrayList();

  private final ChannelManager      genericChannelManager;
  private final String              serverVersion;

  public DSOChannelManagerImpl(ChannelManager genericChannelManager, String serverVersion) {
    this.genericChannelManager = genericChannelManager;
    this.genericChannelManager.addEventListener(new GenericChannelEventListener());
    this.serverVersion = serverVersion;
  }

  public MessageChannel getActiveChannel(NodeID id) throws NoSuchChannelException {
    final MessageChannel rv = (MessageChannel) activeChannels.get(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  public void closeAll(Collection clientIDs) {
    for (Iterator i = clientIDs.iterator(); i.hasNext();) {
      ClientID id = (ClientID) i.next();

      MessageChannel channel = genericChannelManager.getChannel(id.getChannelID());
      if (channel != null) {
        channel.close();
      }
    }
  }

  public MessageChannel[] getActiveChannels() {
    return (MessageChannel[]) activeChannels.valuesToArray();
  }

  public boolean isActiveID(NodeID nodeID) {
    return activeChannels.containsKey(nodeID);
  }

  public String getChannelAddress(NodeID nid) {
    try {
      MessageChannel channel = getActiveChannel(nid);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid)
      throws NoSuchChannelException {
    return (BatchTransactionAcknowledgeMessage) getActiveChannel(nid)
        .createMessage(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE);
  }

  private ClientHandshakeAckMessage newClientHandshakeAckMessage(ClientID clientID) throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(clientID.getChannelID());
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeAckMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
  }

  public Set getAllActiveClientIDs() {
    return activeChannels.keySet();
  }

  public void makeChannelActive(ClientID clientID, boolean persistent, NodeIDImpl serverNodeID) {
    try {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      MessageChannel channel = ackMsg.getChannel();
      synchronized (activeChannels) {
        activeChannels.put(clientID, channel);
        ackMsg.initialize(persistent, getAllActiveClientIDsString(), clientID.toString(),
                          serverVersion, serverNodeID);
        ackMsg.send();
      }
      fireChannelCreatedEvent(channel);
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake message to disconnected client: " + clientID);
    }
  }

  private Set getAllActiveClientIDsString() {
    Set clientIDStrings = new HashSet();
    synchronized (activeChannels) {
      for (Iterator i = activeChannels.keySet().iterator(); i.hasNext();) {
        ClientID cid = (ClientID) i.next();
        clientIDStrings.add(cid.toString());
      }
    }
    return clientIDStrings;
  }

  public void makeChannelActiveNoAck(MessageChannel channel) {
    activeChannels.put(getClientIDFor(channel.getChannelID()), channel);
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
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

  private void fireChannelCreatedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      DSOChannelManagerEventListener eventListener = (DSOChannelManagerEventListener) iter.next();
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      DSOChannelManagerEventListener eventListener = (DSOChannelManagerEventListener) iter.next();
      eventListener.channelRemoved(channel);
    }
  }

  private class GenericChannelEventListener implements ChannelManagerEventListener {

    public void channelCreated(MessageChannel channel) {
      // nothing
    }

    public void channelRemoved(MessageChannel channel) {
      activeChannels.remove(getClientIDFor(channel.getChannelID()));
      fireChannelRemovedEvent(channel);
    }

  }

  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID);
  }

}
