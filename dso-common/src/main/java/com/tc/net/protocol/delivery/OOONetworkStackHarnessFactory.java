/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
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
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(transportFactory, channel, factory, reconnectConfig);
  }

  @Override
  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(channelFactory, transport, factory, reconnectConfig);
  }

}
