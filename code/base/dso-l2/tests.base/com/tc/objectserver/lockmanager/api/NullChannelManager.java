/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.net.DSOChannelManager;

import java.util.Collection;

/**
 * @author steve
 */
public class NullChannelManager implements DSOChannelManager {

  public boolean isValidID(ChannelID channelID) {
    return true;
  }

  public MessageChannel getChannel(ChannelID id) {
    return null;
  }

  public MessageChannel[] getChannels() {
    return null;
  }

  public void closeAll(Collection channelIDs) {
    return;
  }

  public String getChannelAddress(ChannelID channelID) {
    return null;
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) {
    return null;
  }
  
  public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) {
    return null;
  }

  public Collection getAllChannelIDs() {
    return null;
  }

}