/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world and to provide delayed visibility of
 * channels
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean {
  private static final TCLogger         logger              = TCLogging.getLogger(DSOChannelManager.class);
  private static final MessageChannel[] EMPTY_CHANNEL_ARRAY = new MessageChannel[] {};

  private final Map                     activeChannels      = new HashMap();
  private final List                    eventListeners      = new CopyOnWriteArrayList();

  private final ChannelManager          genericChannelManager;

  public DSOChannelManagerImpl(ChannelManager genericChannelManager) {
    this.genericChannelManager = genericChannelManager;
    this.genericChannelManager.addEventListener(new GenericChannelEventListener());
  }

  public MessageChannel getActiveChannel(ChannelID id) throws NoSuchChannelException {
    final MessageChannel rv;
    synchronized (activeChannels) {
      rv = (MessageChannel) activeChannels.get(id);
    }
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  public void closeAll(Collection channelIDs) {
    for (Iterator i = channelIDs.iterator(); i.hasNext();) {
      ChannelID id = (ChannelID) i.next();

      MessageChannel channel = genericChannelManager.getChannel(id);
      if (channel != null) {
        channel.close();
      }
    }
  }

  public MessageChannel[] getActiveChannels() {
    synchronized (activeChannels) {
      return (MessageChannel[]) activeChannels.values().toArray(EMPTY_CHANNEL_ARRAY);
    }
  }

  public boolean isActiveID(ChannelID channelID) {
    synchronized (activeChannels) {
      return activeChannels.containsKey(channelID);
    }
  }

  public String getChannelAddress(ChannelID channelID) {
    try {
      MessageChannel channel = getActiveChannel(channelID);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID)
      throws NoSuchChannelException {
    return (BatchTransactionAcknowledgeMessage) getActiveChannel(channelID)
        .createMessage(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE);
  }

  private ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) throws NoSuchChannelException {
    MessageChannelInternal channel = genericChannelManager.getChannel(channelID);
    if (channel == null) { throw new NoSuchChannelException(); }
    return (ClientHandshakeAckMessage) channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
  }

  public Set getAllActiveChannelIDs() {
    synchronized (activeChannels) {
      return Collections.unmodifiableSet(activeChannels.keySet());
    }
  }

  public void makeChannelActive(ChannelID channelID, long startIDs, long endIDs, boolean persistent) {
    try {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(channelID);
      MessageChannel channel = ackMsg.getChannel();
      synchronized (activeChannels) {
        activeChannels.put(channel.getChannelID(), channel);
        ackMsg.initialize(startIDs, endIDs, persistent, getActiveChannels());
        ackMsg.send();

      }
      fireChannelCreatedEvent(channel);
    } catch (NoSuchChannelException nsce) {
      logger.warn("Not sending handshake message to disconnected client: " + channelID);
    }
  }

  public void makeChannelActiveNoAck(MessageChannel channel) {
    synchronized (activeChannels) {
      activeChannels.put(channel.getChannelID(), channel);
    }
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
    if (listener == null) { throw new NullPointerException("listener cannot be be null"); }
    eventListeners.add(listener);
  }

  public Set getRawChannelIDs() {
    return genericChannelManager.getAllChannelIDs();
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
      synchronized (activeChannels) {
        activeChannels.remove(channel.getChannelID());
      }
      fireChannelRemovedEvent(channel);
    }

  }

}
