/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;

public interface OOOProtocolMessageDelivery {

  public OOOProtocolMessage createHandshakeMessage(long ack);

  public OOOProtocolMessage createHandshakeReplyOkMessage(long ack);

  public OOOProtocolMessage createHandshakeReplyFailMessage(long ack);

  public OOOProtocolMessage createAckMessage(long sequence);

  public boolean sendMessage(OOOProtocolMessage msg);

  public void receiveMessage(OOOProtocolMessage msg);

  public OOOProtocolMessage createProtocolMessage(long sent, TCNetworkMessage msg);

  public ConnectionID getConnectionId();

}
