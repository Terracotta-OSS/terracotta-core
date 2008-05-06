/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;

public interface WireProtocolMessage extends TCNetworkMessage {

  public short getMessageProtocol();

  public WireProtocolHeader getWireProtocolHeader();

  public TCConnection getSource();

}