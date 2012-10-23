/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;

import java.util.HashMap;
import java.util.HashSet;

public class TestConnectionPolicy implements ConnectionPolicy {

  int                                    clientConnected;
  boolean                                maxConnectionsExceeded;
  int                                    maxConnections;
  HashMap<String, HashSet<ConnectionID>> clientsByJvm = new HashMap<String, HashSet<ConnectionID>>();

  @Override
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

  @Override
  public synchronized void clientDisconnected(ConnectionID connID) {

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return; // must have already received the event for this client

    if (jvmClients.remove(connID)) clientConnected--;

    if (jvmClients.size() == 0) {
      clientsByJvm.remove(connID.getJvmID());
    }
  }

  @Override
  public synchronized boolean isMaxConnectionsReached() {
    if (maxConnectionsExceeded) { return maxConnectionsExceeded; }
    return (clientsByJvm.size() >= maxConnections);
  }

  @Override
  public synchronized int getMaxConnections() {
    return maxConnections;
  }

  @Override
  public int getNumberOfActiveConnections() {
    throw new ImplementMe();
  }

  @Override
  public int getConnectionHighWatermark() {
    throw new ImplementMe();
  }

  @Override
  public boolean isConnectAllowed(ConnectionID connID) {
    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return !isMaxConnectionsReached();

    return true;
  }
}
