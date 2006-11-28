/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactory;


public class MockOnceAndOnlyOnceProtocolNetworkLayerFactory implements OnceAndOnlyOnceProtocolNetworkLayerFactory {
  
  public OnceAndOnlyOnceProtocolNetworkLayer layer;
  
  public OnceAndOnlyOnceProtocolNetworkLayer createNewInstance(Sink workSink) {
    return layer;
  }
  
}