/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionPolicy {

  public boolean connectClient(ConnectionID connID);

  public void clientDisconnected(ConnectionID connID);

  public boolean isMaxConnectionsReached();

  public int getMaxConnections();

  public int getNumberOfActiveConnections();

  public int getConnectionHighWatermark();

}
