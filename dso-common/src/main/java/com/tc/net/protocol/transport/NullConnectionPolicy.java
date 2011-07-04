/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class NullConnectionPolicy implements ConnectionPolicy {

  public boolean connectClient(ConnectionID id) {
    return true;
  }

  public void clientDisconnected(ConnectionID id) {
    return;
  }

  public boolean isMaxConnectionsReached() {
    return false;
  }

  public int getMaxConnections() {
    return -1;
  }
}
