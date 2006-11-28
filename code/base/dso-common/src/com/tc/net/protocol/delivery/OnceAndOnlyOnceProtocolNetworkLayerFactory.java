/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;

public interface OnceAndOnlyOnceProtocolNetworkLayerFactory {
  public OnceAndOnlyOnceProtocolNetworkLayer createNewInstance(Sink workSink);
}
