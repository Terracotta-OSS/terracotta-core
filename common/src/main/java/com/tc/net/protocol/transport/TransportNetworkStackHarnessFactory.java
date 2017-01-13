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
package com.tc.net.protocol.transport;

import com.tc.net.protocol.ClientNetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ServerNetworkStackHarness;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;

public class TransportNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  @Override
  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new ServerNetworkStackHarness(channelFactory, transport) {
      @Override
      protected void connectStack() {
        super.connectStack();
// disconnect the receive, this one is send only
        getTransport().setReceiveLayer(null);
      }
    };
  }

  @Override
  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 ClientMessageChannel channel,
                                                 MessageTransportListener[] transportListeners) {
    return new ClientNetworkStackHarness(transportFactory, channel, false){
      @Override
      protected void connectStack() {
        super.connectStack();
// disconnect the receive, this one is send only
        getTransport().setReceiveLayer(null);
      }
    };
  }
}
