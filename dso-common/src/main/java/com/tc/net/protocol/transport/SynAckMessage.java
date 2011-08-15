/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface SynAckMessage extends TransportHandshakeMessage {

  public String getErrorContext();

  public short getErrorType();

  public boolean hasErrorContext();

  public boolean isMaxConnectionsExceeded();

  int getCallbackPort();

  public int getMaxConnections();

}
