/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.net;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.Collection;
import java.util.Set;

/**
 * Wraps the generic ChannelManager adding slightly different channel visibility than DSO requires (we don't want
 * channels to be visible to other subsystems until they have fully handshaked)
 */
public interface DSOChannelManager {

  public void closeAll(Collection<? extends NodeID> clientIDs);

  public MessageChannel getActiveChannel(NodeID id) throws NoSuchChannelException;

  public MessageChannel[] getActiveChannels();

  public boolean isActiveID(NodeID nodeID);

  public String getChannelAddress(NodeID nid);

  public void addEventListener(ChannelManagerEventListener listener);

  public Set<NodeID> getAllClientIDs();

  public void makeChannelActive(ClientID clientID);

  public void makeChannelRefuse(ClientID clientID, String message);

  public void makeChannelActiveNoAck(MessageChannel channel);

  public ClientID getClientIDFor(ChannelID channelID);

}
