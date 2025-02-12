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
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.core.ProductID;
import com.tc.util.Assert;

import java.net.InetSocketAddress;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class ServerMessageChannelImpl extends AbstractMessageChannel implements ServerMessageChannel {
  private static final Logger logger = LoggerFactory.getLogger(ServerMessageChannel.class);
  private final ChannelID       channel;
  /**
   * this is for the server it needs a session ID
   */
  protected ServerMessageChannelImpl(ChannelID channel, TCMessageRouter router, TCMessageFactory msgFactory,
                                     ServerID serverID) {
    super(router, logger, msgFactory);
    this.channel = channel;
    setLocalNodeID(serverID);

    // server message channels should always be open initially
    synchronized (getStatus()) {
      channelOpened();
    }
  }

  @Override
  public ChannelID getChannelID() {
    if (this.isConnected()) {
      Assert.assertEquals(super.getChannelID(), channel);
    }
    return channel;
  }

  @Override
  public NodeID getRemoteNodeID() {
    return getConnectionID().getClientID();
  }

  @Override
  public ProductID getProductID() {
    return getProductID(ProductID.PERMANENT);
  }

  @Override
  public NetworkStackID open(Iterable<InetSocketAddress> serverAddresses) {
    throw new UnsupportedOperationException("Server channels don't support open()");
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Server channels don't support reset()");
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
//  don't bother removing attachment, it will be replaced on reconnect and we could use the info 
//  even if the transport is closed
//    removeAttachment(TRANSPORT_INFO);
    super.notifyTransportClosed(transport); 
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    addAttachment(TRANSPORT_INFO, transport, true);
    super.notifyTransportConnected(transport); 
  }

}