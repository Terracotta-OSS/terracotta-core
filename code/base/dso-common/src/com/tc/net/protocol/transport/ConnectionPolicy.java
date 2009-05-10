/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionPolicy {

  public boolean connectClient(ConnectionID connID);

  public void clientDisconnected(ConnectionID connID);

  public boolean isMaxConnectionsReached();

  public int getMaxConnections();

}