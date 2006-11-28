/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class NullConnectionPolicy implements ConnectionPolicy {

  public void clientConnected() {
    return;
  }

  public void clientDisconnected() {
    return;
  }

  public boolean maxConnectionsExceeded() {
    return false;
  }

  public int getMaxConnections() {
    return -1;
  }

  public void setMaxConnections(int i) {
    return;
  }

}
