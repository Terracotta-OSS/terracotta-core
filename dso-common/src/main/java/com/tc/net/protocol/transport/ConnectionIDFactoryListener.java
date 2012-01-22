/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionIDFactoryListener {

  public void connectionIDCreated(ConnectionID connectionID);

  public void connectionIDDestroyed(ConnectionID connectionID);

}
