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
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import java.util.LinkedList;
import java.util.List;

public class ServerNetworkStackHarness extends LayeredNetworkStackHarness {
  protected MessageTransport                transport;
  protected MessageChannelInternal          channel;
  private final ServerMessageChannelFactory channelFactory;
  private final List<NetworkLayer>          intermediate = new LinkedList<NetworkLayer>();
  private final SetOnceFlag                 finalized = new SetOnceFlag();

  public ServerNetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport) {
    this.channelFactory = channelFactory;
    this.transport = transport;
  }

  /**
   * Connects a new transport to an existing stack (server-side).
   */
  @Override
  public final MessageTransport attachNewConnection(TCConnection connection) throws IllegalReconnectException {
    Assert.eval("Attempt to connect a transport to a stack that hasn't been finalized.", finalized.isSet());
    this.transport.attachNewConnection(connection);
    return this.transport;
  }

  /**
   * Creates and connects a new stack.
   */
  @Override
  public final void finalizeStack() {
    if (finalized.attemptSet()) {
      Assert.assertNotNull(this.transport);
      Assert.assertNotNull(this.channelFactory);
      this.channel = channelFactory.createNewChannel(new ChannelID(this.transport.getConnectionId().getChannelID()),
          transport.getConnectionId().getProductId());

      connectStack();
    } else {
      throw Assert.failure("Attempt to finalize an already finalized stack");
    }
  }

  protected void connectStack() {
    NetworkLayer last = chainNetworkLayers(transport);
    last.setReceiveLayer(channel);
    channel.setSendLayer(last);
    MessageTransportListener listener = createTransportListener(channel);
    transport.addTransportListener(listener);
  }
  /**
   * This listener is added to the transport at the end of connecting the stack
   * @param created
   * @return 
   */
  protected MessageTransportListener createTransportListener(MessageChannelInternal created) {
    return created;
  }
  
  @Override
  public MessageTransport getTransport() {
    return this.transport;
  }

  @Override
  public MessageChannel getChannel() {
    return this.channel;
  }  
   
  @Override
  public String toString() {
    return "ServerNetworkStackHarness[ transport:" + transport + "channel" + channel + "]";
  }
}
