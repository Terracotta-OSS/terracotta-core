/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionWatcher;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;

/**
 * Plain (no guarenteed messages) network stack harness factory
 */
public class PlainNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  private final boolean allowConnectionReplace;

  public PlainNetworkStackHarnessFactory() {
    this(false);
  }

  public PlainNetworkStackHarnessFactory(boolean allowConnectionReplace) {
    this.allowConnectionReplace = allowConnectionReplace;
  }

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new PlainNetworkStackHarness(channelFactory, transport);
  }

  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new PlainNetworkStackHarness(transportFactory, channel);
  }

  private class PlainNetworkStackHarness extends AbstractNetworkStackHarness {

    PlainNetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport) {
      super(channelFactory, transport);
    }

    PlainNetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel) {
      super(transportFactory, channel);
    }

    protected void connectStack() {
      channel.setSendLayer(transport);
      transport.setReceiveLayer(channel);
      transport.setAllowConnectionReplace(allowConnectionReplace);

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

    protected void createIntermediateLayers() {
      // No intermediate layers to create.
    }
  }
}
