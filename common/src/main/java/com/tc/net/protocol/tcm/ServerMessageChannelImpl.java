/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.util.ProductID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.Assert;
import java.util.Collection;

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
      return channel;
    } else {
      return channel;
    }
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
  public NetworkStackID open(Collection<ConnectionInfo> info) {
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