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

  public boolean isActiveID(NodeID nodeID) {
    return true;
  }

  public MessageChannel getActiveChannel(NodeID id) {
    throw new UnsupportedOperationException();
  }

  public MessageChannel[] getActiveChannels() {
    return new MessageChannel[] {};
  }

  public void closeAll(Collection channelIDs) {
    return;
  }

  public String getChannelAddress(NodeID nid) {
    return "";
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
    throw new UnsupportedOperationException();
  }

  public TCConnection[] getAllActiveClientConnections() {
    return new TCConnection[] {};
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
    //
  }

  public void makeChannelActive(ClientID clientID, boolean persistent) {
    //
  }

  public Set getAllClientIDs() {
    return Collections.EMPTY_SET;
  }

  public void makeChannelActiveNoAck(MessageChannel channel) {
    //
  }

  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

  public void makeChannelRefuse(ClientID clientID, String message) {
    //
  }

}