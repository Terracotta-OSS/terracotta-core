/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public int getNumberOfActiveConnections() {
    return 0;
  }

  public int getConnectionHighWatermark() {
    return 0;
  }

}
