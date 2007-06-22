/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.AbstractNetworkStackHarness;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.properties.TCPropertiesImpl;

public class OOONetworkStackHarness extends AbstractNetworkStackHarness {

  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private Sink                                             sink;
  private OnceAndOnlyOnceProtocolNetworkLayer              oooLayer;
  private final boolean                                    isClient;

  OOONetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink) {
    super(channelFactory, transport);
    this.isClient = false;
    this.factory = factory;
    this.sink = sink;
  }

  OOONetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink) {
    super(transportFactory, channel);
    this.isClient = true;
    this.factory = factory;
    this.sink = sink;
  }

  protected void connectStack() {
    channel.setSendLayer(oooLayer);
    oooLayer.setReceiveLayer(channel);
    oooLayer.addTransportListener(channel);

    oooLayer.setSendLayer(transport);
    transport.setReceiveLayer(oooLayer);

    final long timeout = TCPropertiesImpl.getProperties().getLong("l1.reconnect.timeout.millis");
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
