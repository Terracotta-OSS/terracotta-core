/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionIDFactoryListener {

  public void connectionIDCreated(ConnectionID connectionID);

  public void connectionIDDestroyed(ConnectionID connectionID);

}
