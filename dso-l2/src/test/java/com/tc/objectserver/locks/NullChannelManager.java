/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NullChannelManager implements DSOChannelManager {

  @Override
  public boolean isActiveID(NodeID nodeID) {
    return true;
  }

  @Override
  public MessageChannel getActiveChannel(NodeID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MessageChannel[] getActiveChannels() {
    return new MessageChannel[] {};
  }

  @Override
  public void closeAll(Collection channelIDs) {
    return;
  }

  @Override
  public String getChannelAddress(NodeID nid) {
    return "";
  }

  @Override
  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCConnection[] getAllActiveClientConnections() {
    return new TCConnection[] {};
  }

  @Override
  public void addEventListener(DSOChannelManagerEventListener listener) {
    //
  }

  @Override
  public void makeChannelActive(ClientID clientID, boolean persistent) {
    //
  }

  @Override
  public Set getAllClientIDs() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void makeChannelActiveNoAck(MessageChannel channel) {
    //
  }

  @Override
  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

  @Override
  public void makeChannelRefuse(ClientID clientID, String message) {
    //
  }

}