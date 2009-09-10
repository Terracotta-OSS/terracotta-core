/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.util.Assert;

import java.util.HashSet;

public class TestConnectionPolicy implements ConnectionPolicy {

  int                   clientConnected;
  boolean               maxConnectionsExceeded;
  int                   maxConnections;
  HashSet<ConnectionID> clientSet = new HashSet<ConnectionID>();

  public synchronized boolean connectClient(ConnectionID id) {
    if (!isMaxConnectionsReached()) {
      clientSet.add(id);
      clientConnected++;
      return true;
    }
    return false;
  }

  public synchronized void clientDisconnected(ConnectionID id) {
    if (clientSet.remove(id)) {
      Assert.assertTrue(clientConnected > 0);
      clientConnected--;
    }
  }

  public synchronized boolean isMaxConnectionsReached() {
    if (maxConnectionsExceeded) { return maxConnectionsExceeded; }
    return (clientSet.size() >= maxConnections);
  }

  public synchronized int getMaxConnections() {
    return maxConnections;
  }
}
