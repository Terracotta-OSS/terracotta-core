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
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.ClientNetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ServerNetworkStackHarness;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.properties.ReconnectConfig;

public class OOONetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private final ReconnectConfig                            reconnectConfig;

  public OOONetworkStackHarnessFactory(OnceAndOnlyOnceProtocolNetworkLayerFactory factory,
                                       ReconnectConfig reconnectConfig) {
    this.factory = factory;
    this.reconnectConfig = reconnectConfig;
  }

  @Override
  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 ClientMessageChannel channel,
                                                 MessageTransportListener[] transportListeners) {
    final OnceAndOnlyOnceProtocolNetworkLayer layer = factory.createNewClientInstance(reconnectConfig);
    final long timeout = reconnectConfig != null ? reconnectConfig.getReconnectTimeout() : 0;
    layer.addTransportListener(channel);
    ClientNetworkStackHarness harness = new ClientNetworkStackHarness(transportFactory, channel, true) {
      @Override
      protected MessageTransportListener createTransportListener(ClientMessageTransport transport, ClientConnectionEstablisher cce) {
        return new OOOConnectionWatcher(transport, cce, layer, timeout);
      }
    };
    harness.appendIntermediateLayer(layer);
    return harness;
  }

  @Override
  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    transport.setAllowConnectionReplace(true);
    final OnceAndOnlyOnceProtocolNetworkLayer layer = factory.createNewServerInstance(reconnectConfig);
    final long timeout = reconnectConfig != null ? reconnectConfig.getReconnectTimeout() : 0;
    ServerNetworkStackHarness harness = new ServerNetworkStackHarness(channelFactory, transport) {
      @Override
      protected MessageTransportListener createTransportListener(MessageChannelInternal created) {
        layer.addTransportListener(created);
        return new OOOReconnectionTimeout(layer, timeout);
      }
    };
    harness.appendIntermediateLayer(layer);
    return harness;
  }

}
