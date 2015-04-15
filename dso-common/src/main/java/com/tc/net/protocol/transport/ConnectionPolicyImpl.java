/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  private final TCLogger                               logger       = TCLogging.getLogger(ConnectionPolicyImpl.class);
  private final int                                    maxConnections;
  private int                                          maxReached;

  public ConnectionPolicyImpl(int maxConnections) {
    Assert.assertTrue("negative maxConnections", maxConnections >= 0);
    this.maxConnections = maxConnections;
  }

  @Override
  public synchronized boolean isConnectAllowed(ConnectionID connID) {
    if (connID.getProductId().isInternal()) {
      // Don't count internal clients.
      return true;
    }

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null && isMaxConnectionsReached()) {
      return false;
    }

    return true;
  }

  @Override
  public synchronized String toString() {
    return "ConnectionPolicy[maxConnections=" + maxConnections + ", connectedJvmCount=" + clientsByJvm.size() + "]";
  }

  @Override
  public synchronized boolean connectClient(ConnectionID connID) {
    if (connID.getProductId().isInternal()) {
      // Always allow connections from internal products
      return true;
    }

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (isMaxConnectionsReached() && jvmClients == null) {
      logger.info("Rejecting " + connID + "; " + toString());
      return false;
    }

    if (jvmClients == null) {
      jvmClients = new HashSet<ConnectionID>();
      clientsByJvm.put(connID.getJvmID(), jvmClients);
      maxReached = clientsByJvm.size();
      logger.info("Allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }

    if (!jvmClients.contains(connID)) {
      logger.info("New connection [" + connID.getChannelID() + "] from jvm " + connID.getJvmID());
      jvmClients.add(connID);
    }

    return true;
  }

  @Override
  public synchronized void clientDisconnected(ConnectionID connID) {
    if (connID.getProductId().isInternal()) {
      // ignore internal clients
      return;
    }

    // not all times clientSet has connID client disconnect removes the connID. after reconnect timeout, for close event
    // we get here again.

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return; // must have already received the event for this client

    if (jvmClients.remove(connID)) {
      logger.info("Removed connection [" + connID.getChannelID() + "] from jvm " + connID.getJvmID());
    }

    if (jvmClients.size() == 0) {
      clientsByJvm.remove(connID.getJvmID());
      logger.info("De-allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }
  }

  @Override
  public synchronized boolean isMaxConnectionsReached() {
    return (clientsByJvm.size() >= maxConnections);
  }

  @Override
  public synchronized int getMaxConnections() {
    return maxConnections;
  }

  @Override
  public synchronized int getNumberOfActiveConnections() {
    return clientsByJvm.size();
  }

  @Override
  public synchronized int getConnectionHighWatermark() {
    return maxReached;
  }
}
