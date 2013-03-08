/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.Set;

public class DefaultConnectionIdFactory implements ConnectionIDFactory {

  private long         sequence;

  private final String uid = UUID.getUUID().toString();

  @Override
  public synchronized ConnectionID nextConnectionId(String clientJvmID) {
    return new ConnectionID(clientJvmID, sequence++, uid);
  }

  @Override
  public ConnectionID makeConnectionId(String clientJvmID, long channelID) {
    return new ConnectionID(clientJvmID, channelID, uid);
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

}
