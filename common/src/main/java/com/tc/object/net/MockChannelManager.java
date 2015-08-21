/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockChannelManager implements DSOChannelManager {

  private final Map<ClientID, MessageChannel> channels = new HashMap<>();

  public void addChannel(MessageChannel channel) {
    synchronized (channels) {
      this.channels.put(new ClientID(channel.getChannelID().toLong()), channel);
    }
  }

  @Override
  public MessageChannel getActiveChannel(NodeID id) {
    synchronized (channels) {
      return this.channels.get(id);
    }
  }

  @Override
  public MessageChannel[] getActiveChannels() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveID(NodeID nodeID) {
    return true;
  }

  @Override
  public void closeAll(Collection<? extends NodeID> channelIDs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getChannelAddress(NodeID nid) {
    return null;
  }

  @Override
  public TCConnection[] getAllActiveClientConnections() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEventListener(DSOChannelManagerEventListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void makeChannelActive(ClientID clientID, boolean persistent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<NodeID> getAllClientIDs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void makeChannelActiveNoAck(MessageChannel channel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

  @Override
  public void makeChannelRefuse(ClientID clientID, String message) {
    throw new UnsupportedOperationException();

  }

}
