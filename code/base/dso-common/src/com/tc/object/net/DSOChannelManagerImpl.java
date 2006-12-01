/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;

import java.util.Collection;
import java.util.Iterator;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean {

  private final ChannelManager genericChannelManager;

  public DSOChannelManagerImpl(ChannelManager genericChannelManager) {
    this.genericChannelManager = genericChannelManager;
  }

  public MessageChannel getChannel(ChannelID id) throws NoSuchChannelException {
    MessageChannel rv = genericChannelManager.getChannel(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  public void closeAll(Collection channelIDs) {
    for (Iterator i = channelIDs.iterator(); i.hasNext();) {
      ChannelID id = (ChannelID) i.next();
      try {
        MessageChannel channel = getChannel(id);
        channel.close();
      } catch (NoSuchChannelException e) {
        //
      }
    }
  }

  public MessageChannel[] getChannels() {
    return genericChannelManager.getChannels();
  }

  public boolean isValidID(ChannelID channelID) {
    return genericChannelManager.isValidID(channelID);
  }

  public void addEventListener(ChannelManagerEventListener listener) {
    this.genericChannelManager.addEventListener(listener);
  }

  public String getChannelAddress(ChannelID channelID) {
    try {
      MessageChannel channel = getChannel(channelID);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID)
      throws NoSuchChannelException {
    return (BatchTransactionAcknowledgeMessage) getChannel(channelID)
        .createMessage(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE);
  }

  public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) throws NoSuchChannelException {
    return (ClientHandshakeAckMessage) getChannel(channelID).createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
  }

  public Collection getAllChannelIDs() {
    return genericChannelManager.getAllChannelIDs();
  }
}
