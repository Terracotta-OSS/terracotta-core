/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;

import java.util.Collection;

/**
 * Wraps the generic ChannelManager to isolate the rest of the DSO world from it's interface
 */
public interface DSOChannelManager {

  public void closeAll(Collection channelIDs);

  public MessageChannel getChannel(ChannelID id) throws NoSuchChannelException;

  public MessageChannel[] getChannels();

  public boolean isValidID(ChannelID channelID);

  public String getChannelAddress(ChannelID channelID);

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) throws NoSuchChannelException;
  
  public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) throws NoSuchChannelException;

  public Collection getAllChannelIDs();
  
}
