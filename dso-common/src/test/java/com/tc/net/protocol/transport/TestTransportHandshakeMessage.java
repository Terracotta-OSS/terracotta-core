/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;

public abstract class TestTransportHandshakeMessage extends TestWireProtocolMessage implements
    TransportHandshakeMessage {

  public ConnectionID connectionID = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());

  @Override
  public ConnectionID getConnectionId() {
    return connectionID;
  }

  @Override
  public boolean isMaxConnectionsExceeded() {
    throw new ImplementMe();
  }

  @Override
  public int getMaxConnections() {
    throw new ImplementMe();
  }

}
