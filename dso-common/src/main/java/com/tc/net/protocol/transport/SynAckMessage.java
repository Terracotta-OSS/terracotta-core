/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface SynAckMessage extends TransportHandshakeMessage {

  public String getErrorContext();

  public short getErrorType();

  public boolean hasErrorContext();

  @Override
  public boolean isMaxConnectionsExceeded();

  int getCallbackPort();

  @Override
  public int getMaxConnections();

}
