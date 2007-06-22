/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactory;

public class MockOnceAndOnlyOnceProtocolNetworkLayerFactory implements OnceAndOnlyOnceProtocolNetworkLayerFactory {

  public OnceAndOnlyOnceProtocolNetworkLayer layer;

  public OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(Sink workSink) {
    return layer;
  }

  public OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(Sink workSink) {
    return layer;
  }
}
