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