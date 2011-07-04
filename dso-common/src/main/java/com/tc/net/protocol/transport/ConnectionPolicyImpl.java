/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.util.HashSet;

public class ConnectionPolicyImpl implements ConnectionPolicy {

  private HashSet<ConnectionID> clientSet = new HashSet<ConnectionID>();
  private int                   maxConnections;
  private TCLogger              logger    = TCLogging.getLogger(ConnectionPolicyImpl.class);

  public ConnectionPolicyImpl(int maxConnections) {
    Assert.assertTrue("negative maxConnections", maxConnections >= 0);
    this.maxConnections = maxConnections;
  }

  public synchronized String toString() {
    return "ConnectionPolicy[maxConnections=" + maxConnections + ", connectionCount=" + clientSet.size() + "]";
  }

  public synchronized boolean connectClient(ConnectionID connID) {
    if (clientSet.contains(connID) || !isMaxConnectionsReached()) {
      clientSet.add(connID);
      return true;
    } else {
      logger.info("Rejecting " + connID + "; " + toString());
      return false;
    }
  }

  public synchronized void clientDisconnected(ConnectionID connID) {
    // not all times clientSet has connID client disconnect removes the connID. after reconnect timeout, for close event
    // we get here again.
    clientSet.remove(connID);
  }

  public synchronized boolean isMaxConnectionsReached() {
    return (clientSet.size() >= maxConnections);
  }

  public synchronized int getMaxConnections() {
    return maxConnections;
  }
}
