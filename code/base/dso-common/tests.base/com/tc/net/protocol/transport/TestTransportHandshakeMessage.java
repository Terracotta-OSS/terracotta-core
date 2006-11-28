/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;

public abstract class TestTransportHandshakeMessage extends TestWireProtocolMessage implements
    TransportHandshakeMessage {

  public ConnectionID connectionID = ConnectionID.NULL_ID;

  public ConnectionID getConnectionId() {
    return connectionID;
  }

  public boolean isMaxConnectionsExceeded() {
    throw new ImplementMe();
  }

  public int getMaxConnections() {
    throw new ImplementMe();
  }

}
