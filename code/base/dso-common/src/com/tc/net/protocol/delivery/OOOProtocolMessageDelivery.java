/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;

public interface OOOProtocolMessageDelivery {
  public void sendAckRequest();

  public void sendAck(long sequence);

  public void sendMessage(OOOProtocolMessage msg);

  public void receiveMessage(OOOProtocolMessage msg);
  
  public OOOProtocolMessage createProtocolMessage(long sent, TCNetworkMessage msg);
}