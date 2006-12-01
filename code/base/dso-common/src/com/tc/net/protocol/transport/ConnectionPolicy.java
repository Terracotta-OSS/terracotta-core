/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionPolicy {

  public void clientConnected();

  public void clientDisconnected();

  public boolean maxConnectionsExceeded();

  public int getMaxConnections();

  public void setMaxConnections(int i);

}