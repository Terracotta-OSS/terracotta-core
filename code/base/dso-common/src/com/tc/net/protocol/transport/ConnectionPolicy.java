/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionPolicy {

  public void clientConnected();

  public void clientDisconnected();

  public boolean maxConnectionsExceeded();

  public int getMaxConnections();

  public void setMaxConnections(int i);

}