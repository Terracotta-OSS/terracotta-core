/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;


public interface SynAckMessage extends TransportHandshakeMessage {

  public String getErrorContext();

  public boolean hasErrorContext();

  public boolean isMaxConnectionsExceeded();

  public int getMaxConnections();

}
