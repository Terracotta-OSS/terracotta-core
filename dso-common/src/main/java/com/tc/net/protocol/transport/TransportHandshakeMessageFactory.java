/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

public interface TransportHandshakeMessageFactory {

  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source, short stackLayerFlags,
                                             int callbackPort);

  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source);

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections, int callbackPort);

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections);
}
