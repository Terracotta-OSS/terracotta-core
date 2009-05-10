/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.util.Assert;

import java.util.HashSet;

public class TestConnectionPolicy implements ConnectionPolicy {

  public int                    clientConnected;
  public boolean                maxConnectionsExceeded;
  public int                    maxConnections;
  private HashSet<ConnectionID> clientSet = new HashSet<ConnectionID>();

  public boolean connectClient(ConnectionID id) {
    if (!isMaxConnectionsReached()) {
      clientSet.add(id);
      clientConnected++;
      return true;
    }
    return false;
  }

  public void clientDisconnected(ConnectionID id) {
    if (clientSet.remove(id)) {
      Assert.assertTrue(clientConnected > 0);
      clientConnected--;
    }
  }

  public boolean isMaxConnectionsReached() {
    return maxConnectionsExceeded;
  }

  public int getMaxConnections() {
    return maxConnections;
  }
}
