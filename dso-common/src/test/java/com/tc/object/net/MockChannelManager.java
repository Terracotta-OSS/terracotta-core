/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockChannelManager implements DSOChannelManager {

  private final Map channels = new HashMap();

  public void addChannel(MessageChannel channel) {
    synchronized (channels) {
      this.channels.put(new ClientID(channel.getChannelID().toLong()), channel);
    }
  }

  @Override
  public MessageChannel getActiveChannel(NodeID id) {
    synchronized (channels) {
      return (MessageChannel) this.channels.get(id);
    }
  }

  @Override
  public MessageChannel[] getActiveChannels() {
    throw new ImplementMe();
  }

  @Override
  public boolean isActiveID(NodeID nodeID) {
    return true;
  }

  @Override
  public void closeAll(Collection channelIDs) {
    throw new ImplementMe();
  }

  @Override
  public String getChannelAddress(NodeID nid) {
    return null;
  }

  @Override
  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
    throw new ImplementMe();
  }

  @Override
  public TCConnection[] getAllActiveClientConnections() {
    throw new ImplementMe();
  }

  @Override
  public void addEventListener(DSOChannelManagerEventListener listener) {
    throw new ImplementMe();
  }

  @Override
  public void makeChannelActive(ClientID clientID, boolean persistent) {
    throw new ImplementMe();
  }

  @Override
  public Set getAllClientIDs() {
    throw new ImplementMe();
  }

  @Override
  public void makeChannelActiveNoAck(MessageChannel channel) {
    throw new ImplementMe();
  }

  @Override
  public ClientID getClientIDFor(ChannelID channelID) {
    return new ClientID(channelID.toLong());
  }

  @Override
  public void makeChannelRefuse(ClientID clientID, String message) {
    throw new ImplementMe();

  }

}
