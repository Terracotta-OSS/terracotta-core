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
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import static com.tc.net.protocol.tcm.ServerMessageChannel.TRANSPORT_INFO;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

public class ServerNetworkStackHarness extends LayeredNetworkStackHarness {
  protected MessageTransport                transport;
  protected MessageChannelInternal          channel;
  private final ServerMessageChannelFactory channelFactory;
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
      this.channel = channelFactory.createNewChannel(new ChannelID(transport.getConnectionID().getChannelID()));
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
    channel.addAttachment(TRANSPORT_INFO, transport, true);
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
