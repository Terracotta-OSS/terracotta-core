/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;

import java.util.Collection;
import java.util.Set;

public class TestDSOChannelManager implements DSOChannelManager {

  private final MessageChannel[] allChannels = new MessageChannel[0];

  public void closeAll(Collection channelIDs) {
    throw new ImplementMe();
  }

  public Collection getAllChannelIDs() {
    throw new ImplementMe();
  }

  public MessageChannel getChannel(ChannelID id) {
    throw new ImplementMe();
  }

  public String getChannelAddress(NodeID nid) {
    throw new ImplementMe();
  }

  public MessageChannel[] getChannels() {
    return allChannels;
  }

  public boolean isValidID(ChannelID channelID) {
    throw new ImplementMe();
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
    throw new ImplementMe();
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
    throw new ImplementMe();
  }

  public MessageChannel getActiveChannel(NodeID id) {
    throw new ImplementMe();
  }

  public MessageChannel[] getActiveChannels() {
    return allChannels;
  }

  public TCConnection[] getAllActiveClientConnections() {
    throw new ImplementMe();
  }

  public Set getAllClientIDs() {
    throw new ImplementMe();
  }

  public boolean isActiveID(NodeID nodeID) {
    throw new ImplementMe();
  }

  public void makeChannelActive(ClientID clientID, boolean persistent) {
    throw new ImplementMe();
  }

  public void makeChannelActiveNoAck(MessageChannel channel) {
    throw new ImplementMe();
  }

  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

  public void makeChannelRefuse(ClientID clientID, String message) {
    throw new ImplementMe();

  }
}
