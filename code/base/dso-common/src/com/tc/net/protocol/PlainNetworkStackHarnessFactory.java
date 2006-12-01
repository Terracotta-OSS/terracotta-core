/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;

/**
 * Plain (no guarenteed messages) network stack harness factory
 */
public class PlainNetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new PlainNetworkStackHarness(channelFactory, transport);
  }

  public NetworkStackHarness clientClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new PlainNetworkStackHarness(transportFactory, channel);
  }

  private static class PlainNetworkStackHarness extends AbstractNetworkStackHarness {

    PlainNetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport) {
      super(channelFactory, transport);
    }

    PlainNetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel) {
      super(transportFactory, channel);
    }

    protected void connectStack() {
      channel.setSendLayer(transport);
      transport.setReceiveLayer(channel);
      transport.addTransportListener(channel);
    }

    protected void createIntermediateLayers() {
      // No intermediate layers to create.
    }
  }
}