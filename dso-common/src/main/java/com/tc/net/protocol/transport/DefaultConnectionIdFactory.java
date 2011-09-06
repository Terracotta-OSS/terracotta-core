/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.Set;

public class DefaultConnectionIdFactory implements ConnectionIDFactory {

  private long         sequence;

  private final String uid = UUID.getUUID().toString();

  public synchronized ConnectionID nextConnectionId(String clientJvmID) {
    return new ConnectionID(clientJvmID, sequence++, uid);
  }

  public ConnectionID makeConnectionId(String clientJvmID, long channelID) {
    return new ConnectionID(clientJvmID, channelID, uid);
  }

  public Set loadConnectionIDs() {
    return Collections.EMPTY_SET;
  }

  public void init(String clusterID, long nextAvailChannelID, Set connections) {
    throw new ImplementMe();
  }

  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    throw new ImplementMe();
  }

  public void restoreConnectionId(ConnectionID rv) {
    //
  }

  public long getCurrentConnectionID() {
    return sequence;
  }

}
