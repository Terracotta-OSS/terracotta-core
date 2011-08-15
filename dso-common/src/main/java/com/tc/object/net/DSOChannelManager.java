/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;

import java.util.Collection;
import java.util.Set;

/**
 * Wraps the generic ChannelManager adding slightly different channel visibility than DSO requires (we don't want
 * channels to be visible to other subsystems until they have fully handshaked)
 */
public interface DSOChannelManager {

  public void closeAll(Collection clientIDs);

  public MessageChannel getActiveChannel(NodeID id) throws NoSuchChannelException;

  public MessageChannel[] getActiveChannels();

  public boolean isActiveID(NodeID nodeID);

  public String getChannelAddress(NodeID nid);

  public TCConnection[] getAllActiveClientConnections();

  public void addEventListener(DSOChannelManagerEventListener listener);

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid)
      throws NoSuchChannelException;

  public Set getAllClientIDs();

  public void makeChannelActive(ClientID clientID, boolean persistent);

  public void makeChannelRefuse(ClientID clientID, String message);

  public void makeChannelActiveNoAck(MessageChannel channel);

  public ClientID getClientIDFor(ChannelID channelID);

}
