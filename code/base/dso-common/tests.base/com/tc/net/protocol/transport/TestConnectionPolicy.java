/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;

public class TestConnectionPolicy implements ConnectionPolicy {

  int                                    clientConnected;
  boolean                                maxConnectionsExceeded;
  int                                    maxConnections;
  HashMap<String, HashSet<ConnectionID>> clientsByJvm = new HashMap<String, HashSet<ConnectionID>>();

  public synchronized boolean connectClient(ConnectionID connID) {
    if (isMaxConnectionsReached()) { return false; }

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) {
      jvmClients = new HashSet<ConnectionID>();
      clientsByJvm.put(connID.getJvmID(), jvmClients);
    }

    if (!jvmClients.contains(connID)) {
      clientConnected++;
      jvmClients.add(connID);
    }

    return true;
  }

  public synchronized void clientDisconnected(ConnectionID connID) {

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return; // must have already received the event for this client

    if (jvmClients.remove(connID)) clientConnected--;

    if (jvmClients.size() == 0) {
      clientsByJvm.remove(connID.getJvmID());
      Assert.assertTrue(clientConnected > 0);
    }
  }

  public synchronized boolean isMaxConnectionsReached() {
    if (maxConnectionsExceeded) { return maxConnectionsExceeded; }
    return (clientsByJvm.size() >= maxConnections);
  }

  public synchronized int getMaxConnections() {
    return maxConnections;
  }

  public int getNumberOfActiveConnections() {
    throw new ImplementMe();
  }

  public int getConnectionHighWatermark() {
    throw new ImplementMe();
  }
}
