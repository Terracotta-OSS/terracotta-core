/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.net.protocol.transport;

import com.tc.net.protocol.ClientNetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ServerNetworkStackHarness;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;

public class TransportNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  @Override
  public ServerNetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
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
  public ClientNetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 ClientMessageChannel channel,
                                                 MessageTransportListener[] transportListeners) {
    return new ClientNetworkStackHarness(transportFactory, channel){
      @Override
      protected void connectStack() {
        super.connectStack();
// disconnect the receive, this one is send only
        getTransport().setReceiveLayer(null);
      }
    };
  }
}
