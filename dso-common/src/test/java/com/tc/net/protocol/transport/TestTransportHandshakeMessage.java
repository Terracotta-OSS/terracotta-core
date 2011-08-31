/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;

public abstract class TestTransportHandshakeMessage extends TestWireProtocolMessage implements
    TransportHandshakeMessage {

  public ConnectionID connectionID = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());

  public ConnectionID getConnectionId() {
    return connectionID;
  }

  public boolean isMaxConnectionsExceeded() {
    throw new ImplementMe();
  }

  public int getMaxConnections() {
    throw new ImplementMe();
  }

}
