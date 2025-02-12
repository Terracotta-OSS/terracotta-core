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

import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionWatcher;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportInitiator;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientNetworkStackHarness extends LayeredNetworkStackHarness {
  protected ClientMessageTransport          transport;
  protected ClientMessageChannel            channel;
  private final MessageTransportFactory     transportFactory;
  private final SetOnceFlag                 finalized = new SetOnceFlag();

  public ClientNetworkStackHarness(MessageTransportFactory transportFactory, ClientMessageChannel channel) {
    this.transportFactory = transportFactory;
    this.channel = channel;
  }

  /**
   * Connects a new transport to an existing stack (server-side).
   */
  @Override
  public final ClientMessageTransport attachNewConnection(TCConnection connection) throws IllegalReconnectException {
    throw new UnsupportedOperationException("this is a server-side operation");
  }

  /**
   * Creates and connects a new stack.
   */
  @Override
  public final void finalizeStack() {
    if (finalized.attemptSet()) {
      Assert.assertNotNull(this.channel);
      Assert.assertNotNull(this.transportFactory);
      this.transport = transportFactory.createNewTransport();
      connectStack();
    } else {
      throw Assert.failure("Attempt to finalize an already finalized stack");
    }
  }

  protected void connectStack() {
    final NetworkLayer last = chainNetworkLayers(transport);
  //  connect up the channel the end of the chain
    last.setReceiveLayer(channel);
    
    final ClientConnectionEstablisher cce = new ClientConnectionEstablisher(transport);
    channel.setMessageTransportInitiator(new MessageTransportInitiator() {
      @Override
      public NetworkStackID openMessageTransport(Iterable<InetSocketAddress> serverAddresses, ConnectionID connection) throws CommStackMismatchException, IOException, MaxConnectionsExceededException, TCTimeoutException {
//  this is terrible but it sets the send layer of the channel (which is calling open),
//  sets the connection id in the transport layer, and initiates the connection establisher
//  to maintain the transport layer under the channel
        channel.setSendLayer(last);
        transport.initConnectionID(connection);
        return cce.open(serverAddresses, channel);
      }
    });
    
    MessageTransportListener watcher = new ConnectionWatcher(channel, cce);
    transport.addTransportListener(watcher);
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
    return "ClientNetworkStackHarness[ transport:" + transport + "channel" + channel + "]";
  }
}
