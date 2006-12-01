/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ConnectionPolicyImpl implements ConnectionPolicy {

  private int maxConnections;
  private int connectionCount;
  
  public ConnectionPolicyImpl(int maxConnections) {
    this.maxConnections = maxConnections;
  }

  public synchronized String toString() {
    return "ConnectionPolicy[maxConnections=" + maxConnections + ", connectionCount=" + connectionCount + "]";
  }
  
  public synchronized void clientConnected() {
    connectionCount++;
  }

  public synchronized void clientDisconnected() {
    if (connectionCount == 0) {
      throw new AssertionError("Attempt to decrement connection count below 0.");
    }
    connectionCount--;
  }
  
  public synchronized boolean maxConnectionsExceeded() {
    return false;
  }

  public synchronized int getMaxConnections() {
    return -1;
  }

  public synchronized void setMaxConnections(int i) {
    this.maxConnections = i;
  }
  
}
