/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;

public interface WireProtocolMessage extends TCNetworkMessage {

  public short getMessageProtocol();

  public WireProtocolHeader getWireProtocolHeader();

  public TCConnection getSource();

}
