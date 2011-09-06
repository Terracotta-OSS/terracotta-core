/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.properties.ReconnectConfig;

public interface OnceAndOnlyOnceProtocolNetworkLayerFactory {
  public OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(ReconnectConfig reconnectConfig);

  public OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(ReconnectConfig reconnectConfig);
}
