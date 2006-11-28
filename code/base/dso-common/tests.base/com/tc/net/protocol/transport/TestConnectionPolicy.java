/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class TestConnectionPolicy implements ConnectionPolicy {

  public int clientConnected;
  public int clientDisconnected;
  public boolean maxConnectionsExceeded;
  public int maxConnections;
  
  public void clientConnected() {
    clientConnected++;
  }

  public void clientDisconnected() {
    clientDisconnected++;
  }

  public boolean maxConnectionsExceeded() {
    return maxConnectionsExceeded;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(int i) {
    return;
  }

}
