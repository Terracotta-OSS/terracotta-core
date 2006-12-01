/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
