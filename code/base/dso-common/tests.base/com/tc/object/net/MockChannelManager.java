/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockChannelManager implements DSOChannelManager {

  private Map channels = new HashMap();

  public void addChannel(MessageChannel channel) {
    synchronized (channels) {
      this.channels.put(new ClientID(channel.getChannelID()), channel);
    }
  }

  public MessageChannel getActiveChannel(NodeID id) {
    synchronized (channels) {
      return (MessageChannel) this.channels.get(id);
    }
  }

  public MessageChannel[] getActiveChannels() {
    throw new ImplementMe();
  }

  public boolean isActiveID(NodeID nodeID) {
    return true;
  }

  public void closeAll(Collection channelIDs) {
    throw new ImplementMe();
  }

  public String getChannelAddress(NodeID nid) {
    return null;
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
    throw new ImplementMe();
  }

  public Set getAllActiveClientIDs() {
    throw new ImplementMe();
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
    throw new ImplementMe();
  }

  public void makeChannelActive(ClientID clientID, boolean persistent) {
    throw new ImplementMe();
  }

  public Set getAllClientIDs() {
    throw new ImplementMe();
  }

  public void makeChannelActiveNoAck(MessageChannel channel) {
    throw new ImplementMe();
  }

  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID);
  }

}