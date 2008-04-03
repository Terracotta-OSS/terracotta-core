/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.l1propertiesfroml2.L1ReconnectConfig;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;

public class OOONetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  private final Sink                                       sink;
  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private final L1ReconnectConfig                      l1ReconnectConfig;

  public OOONetworkStackHarnessFactory(OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink,
                                       L1ReconnectConfig l1ReconnectConfig) {
    this.factory = factory;
    this.sink = sink;
    this.l1ReconnectConfig = l1ReconnectConfig;
  }

  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(transportFactory, channel, factory, sink, l1ReconnectConfig);
  }

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(channelFactory, transport, factory, sink, l1ReconnectConfig);
  }

}
