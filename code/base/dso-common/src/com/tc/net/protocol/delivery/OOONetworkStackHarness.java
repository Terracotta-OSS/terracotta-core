/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.l1propertiesfroml2.ReconnectConfig;
import com.tc.net.protocol.AbstractNetworkStackHarness;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;

public class OOONetworkStackHarness extends AbstractNetworkStackHarness {

  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private Sink                                             sink;
  private OnceAndOnlyOnceProtocolNetworkLayer              oooLayer;
  private final boolean                                    isClient;
  private final ReconnectConfig                            reconnectConfig;

  OOONetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink, ReconnectConfig reconnectConfig) {
    super(channelFactory, transport);
    this.isClient = false;
    this.factory = factory;
    this.sink = sink;
    this.reconnectConfig = reconnectConfig;
  }

  OOONetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink, ReconnectConfig reconnectConfig) {
    super(transportFactory, channel);
    this.isClient = true;
    this.factory = factory;
    this.sink = sink;
    this.reconnectConfig = reconnectConfig;
  }

  protected void connectStack() {
    channel.setSendLayer(oooLayer);
    oooLayer.setReceiveLayer(channel);
    oooLayer.addTransportListener(channel);

    transport.setAllowConnectionReplace(true);

    oooLayer.setSendLayer(transport);
    transport.setReceiveLayer(oooLayer);

    long timeout = 0;
    if (reconnectConfig.getReconnectEnabled()) timeout = reconnectConfig.getReconnectTimeout();
    // XXX: this is super ugly, but...
    if (transport instanceof ClientMessageTransport) {
      ClientMessageTransport cmt = (ClientMessageTransport) transport;
      ClientConnectionEstablisher cce = cmt.getConnectionEstablisher();
      OOOConnectionWatcher cw = new OOOConnectionWatcher(cmt, cce, oooLayer, timeout);
      cmt.addTransportListener(cw);
    } else {
      OOOReconnectionTimeout ort = new OOOReconnectionTimeout(oooLayer, timeout);
      transport.addTransportListener(ort);
    }
  }

  protected void createIntermediateLayers() {
    oooLayer = (isClient) ? factory.createNewClientInstance(sink) : factory.createNewServerInstance(sink);
  }
}
