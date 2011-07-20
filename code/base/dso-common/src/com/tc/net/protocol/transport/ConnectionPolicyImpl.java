/*
 * All content copyright (c) 2003-2011 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Enforces max connections (licenses) based on using one license per unique JVM.
 */
public class ConnectionPolicyImpl implements ConnectionPolicy {

  private final HashMap<String, HashSet<ConnectionID>> clientsByJvm = new HashMap<String, HashSet<ConnectionID>>();

  private final int                                    maxConnections;
  private final TCLogger                               logger       = TCLogging.getLogger(ConnectionPolicyImpl.class);

  public ConnectionPolicyImpl(int maxConnections) {
    Assert.assertTrue("negative maxConnections", maxConnections >= 0);
    this.maxConnections = maxConnections;
  }

  @Override
  public synchronized String toString() {
    return "ConnectionPolicy[maxConnections=" + maxConnections + ", connectedJvmCount=" + clientsByJvm.size() + "]";
  }

  public synchronized boolean connectClient(ConnectionID connID) {

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (isMaxConnectionsReached() && jvmClients == null) {
      logger.info("Rejecting " + connID + "; " + toString());
      return false;
    }

    if (jvmClients == null) {
      jvmClients = new HashSet<ConnectionID>();
      clientsByJvm.put(connID.getJvmID(), jvmClients);
      logger.info("Allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }

    if (!jvmClients.contains(connID)) jvmClients.add(connID);

    return true;
  }

  public synchronized void clientDisconnected(ConnectionID connID) {
    // not all times clientSet has connID client disconnect removes the connID. after reconnect timeout, for close event
    // we get here again.

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return; // must have already received the event for this client

    jvmClients.remove(connID);

    if (jvmClients.size() == 0) {
      clientsByJvm.remove(connID.getJvmID());
      logger.info("De-allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }
  }

  public synchronized boolean isMaxConnectionsReached() {
    return (clientsByJvm.size() >= maxConnections);
  }

  public synchronized int getMaxConnections() {
    return maxConnections;
  }

  public synchronized int getNumberOfActiveConnections() {
    return clientsByJvm.size();
  }
}
