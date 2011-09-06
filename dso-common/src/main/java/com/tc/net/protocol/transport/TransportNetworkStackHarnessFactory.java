/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.AbstractNetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;

public class TransportNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new TransportNetworkStackHarness(channelFactory, transport);
  }

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
