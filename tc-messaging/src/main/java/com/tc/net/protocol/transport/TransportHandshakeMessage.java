/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface TransportHandshakeMessage extends WireProtocolMessage {

  public static final int NO_CALLBACK_PORT = -1;

  public ConnectionID getConnectionId();

  public boolean isMaxConnectionsExceeded();

  public int getMaxConnections();

  // XXX: Yuck.
  public boolean isSyn();

  public boolean isSynAck();

  public boolean isAck();

  public short getStackLayerFlags();
}
