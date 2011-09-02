/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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