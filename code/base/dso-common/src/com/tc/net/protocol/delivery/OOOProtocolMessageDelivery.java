/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;

public interface OOOProtocolMessageDelivery {
  public OOOProtocolMessage createAckRequestMessage(short s);

  public OOOProtocolMessage createAckMessage(long sequence);

  public void sendMessage(OOOProtocolMessage msg);

  public void receiveMessage(OOOProtocolMessage msg);

  public OOOProtocolMessage createProtocolMessage(long sent, short sessionId, TCNetworkMessage msg);

  public ConnectionID getConnectionId();

}