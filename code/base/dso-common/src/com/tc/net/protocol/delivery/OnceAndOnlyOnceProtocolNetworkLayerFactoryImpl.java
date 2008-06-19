/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.properties.ReconnectConfig;

/**
 * Creates new instances of OnceAndOnlyOnceProtocolNetworkLayers. This is used so that a mock one may be injected into
 * the once and only once network stack harness for testing.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl implements OnceAndOnlyOnceProtocolNetworkLayerFactory {

  public OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(Sink sendSink, Sink receiveSink,
                                                                     ReconnectConfig reconnectConfig) {
    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageFactory();
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageParser(messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, sendSink, receiveSink,
                                                       reconnectConfig, true);
  }

  public OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(Sink sendSink, Sink receiveSink,
                                                                     ReconnectConfig reconnectConfig) {
    OOOProtocolMessageFactory messageFactory = new OOOProtocolMessageFactory();
    OOOProtocolMessageParser messageParser = new OOOProtocolMessageParser(messageFactory);
    return new OnceAndOnlyOnceProtocolNetworkLayerImpl(messageFactory, messageParser, sendSink, receiveSink,
                                                       reconnectConfig, false);
  }

}
