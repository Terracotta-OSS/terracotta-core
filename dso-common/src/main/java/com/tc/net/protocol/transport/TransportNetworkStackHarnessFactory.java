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
package com.tc.net.protocol.transport;

import com.tc.net.protocol.AbstractNetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;

public class TransportNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  @Override
  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new TransportNetworkStackHarness(channelFactory, transport);
  }

  @Override
  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new TransportNetworkStackHarness(transportFactory, channel);
  }

  private static class TransportNetworkStackHarness extends AbstractNetworkStackHarness {

    TransportNetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport) {
      super(channelFactory, transport);
    }

    TransportNetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel) {
      super(transportFactory, channel);
    }

    @Override
    protected void connectStack() {
      transport.setReceiveLayer(null);

      // XXX: this is super ugly, but...
      if (transport instanceof ClientMessageTransport) {
        ClientMessageTransport cmt = (ClientMessageTransport) transport;
        ClientConnectionEstablisher cce = cmt.getConnectionEstablisher();
        ConnectionWatcher cw = new ConnectionWatcher(cmt, channel, cce);
        transport.addTransportListener(cw);
      } else {
        transport.addTransportListener(channel);
      }
    }

    @Override
    protected void createIntermediateLayers() {
      // no intermediate layers to create
    }

  }
}
