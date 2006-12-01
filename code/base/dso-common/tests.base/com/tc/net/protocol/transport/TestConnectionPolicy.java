/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
