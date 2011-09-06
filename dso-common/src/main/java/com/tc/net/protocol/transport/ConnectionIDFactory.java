/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import java.util.Set;

public interface ConnectionIDFactory {

  public long getCurrentConnectionID();

  public ConnectionID nextConnectionId(String clientJvmId);

  public ConnectionID makeConnectionId(String clientJvmid, long channelID);

  public void restoreConnectionId(ConnectionID rv);

  public Set loadConnectionIDs();

  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener);

  public void init(String clusterID, long nextAvailChannelID, Set connections);

}
