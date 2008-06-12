/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.properties.ReconnectConfig;

public class OOONetworkStackHarnessFactory implements NetworkStackHarnessFactory {

  private final Sink                                       sink;
  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private final ReconnectConfig                            reconnectConfig;
  private final int                                        sendQueueCap;

  public OOONetworkStackHarnessFactory(OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink,
                                       ReconnectConfig reconnectConfig, int sendQueueCap) {
    this.factory = factory;
    this.sink = sink;
    this.reconnectConfig = reconnectConfig;
    this.sendQueueCap = sendQueueCap;
  }

  public NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory,
                                                 MessageChannelInternal channel,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(transportFactory, channel, factory, sink, reconnectConfig, sendQueueCap);
  }

  public NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory,
                                                 MessageTransport transport,
                                                 MessageTransportListener[] transportListeners) {
    return new OOONetworkStackHarness(channelFactory, transport, factory, sink, reconnectConfig, sendQueueCap);
  }

}
