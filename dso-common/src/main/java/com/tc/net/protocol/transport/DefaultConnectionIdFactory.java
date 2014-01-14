/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.Set;

public class DefaultConnectionIdFactory implements ConnectionIDFactory {

  private long         sequence;

  private final String serverID = UUID.getUUID().toString();

  @Override
  public ConnectionID populateConnectionID(final ConnectionID connectionID) {
    if (new ChannelID(connectionID.getChannelID()).isNull()) {
      return nextConnectionId(connectionID.getJvmID());
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID());
    }
  }

  private synchronized ConnectionID nextConnectionId(String clientJvmID) {
    return new ConnectionID(clientJvmID, sequence++, serverID);
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID) {
    return new ConnectionID(clientJvmID, channelID, serverID);
  }

  @Override
  public Set<ConnectionID> loadConnectionIDs() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void init(String clusterID, long nextAvailChannelID, Set<ConnectionID> connections) {
    throw new ImplementMe();
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    throw new ImplementMe();
  }

  @Override
  public void restoreConnectionId(ConnectionID rv) {
    //
  }

  @Override
  public long getCurrentConnectionID() {
    return sequence;
  }

  public String getServerID() {
    return serverID;
  }
}
