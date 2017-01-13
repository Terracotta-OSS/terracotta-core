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

import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ConnectionInfo;
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
import java.util.Collection;

public class ClientNetworkStackHarness extends LayeredNetworkStackHarness {
  protected ClientMessageTransport          transport;
  protected ClientMessageChannel            channel;
  private final MessageTransportFactory     transportFactory;
  private final SetOnceFlag                 finalized = new SetOnceFlag();
  private final boolean                     allowConnectionReplace;

  public ClientNetworkStackHarness(MessageTransportFactory transportFactory, ClientMessageChannel channel, boolean allowConnectionReplace) {
    this.transportFactory = transportFactory;
    this.channel = channel;
    this.allowConnectionReplace = allowConnectionReplace;
  }

  /**
   * Connects a new transport to an existing stack (server-side).
   */
  @Override
  public final ClientMessageTransport attachNewConnection(TCConnection connection) throws IllegalReconnectException {
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
      Assert.assertNotNull(this.channel);
      Assert.assertNotNull(this.transportFactory);
      this.transport = transportFactory.createNewTransport();
      this.transport.setAllowConnectionReplace(allowConnectionReplace);
      connectStack();
    } else {
      throw Assert.failure("Attempt to finalize an already finalized stack");
    }
  }
  
  protected ClientConnectionEstablisher createClientConnectionEstablisher() {
    return transportFactory.createClientConnectionEstablisher();
  }

  protected void connectStack() {
    final NetworkLayer last = chainNetworkLayers(transport);
  //  connect up the channel the end of the chain
    last.setReceiveLayer(channel);
    
    final ClientConnectionEstablisher cce = createClientConnectionEstablisher();
    channel.setMessageTransportInitiator(new MessageTransportInitiator() {
      @Override
      public NetworkStackID openMessageTransport(Collection<ConnectionInfo> dest, ConnectionID connection) throws CommStackMismatchException, IOException, MaxConnectionsExceededException, TCTimeoutException {
//  this is terrible but it sets the send layer of the channel (which is calling open),
//  sets the connection id in the transport layer, and initiates the connection establisher
//  to maintain the transport layer under the channel
        channel.setSendLayer(last);
        transport.initConnectionID(connection);
        return cce.open(dest, transport);
      }
    });
    
    MessageTransportListener watcher = createTransportListener(transport, cce);
    transport.addTransportListener(watcher);
  }
  
  protected MessageTransportListener createTransportListener(ClientMessageTransport transport, ClientConnectionEstablisher cce) {
    return new ConnectionWatcher(transport, channel, cce);
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
