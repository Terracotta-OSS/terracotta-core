/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;

public interface OOOProtocolMessageDelivery {
  public OOOProtocolMessage createAckRequestMessage();
  public void sendAckRequest();

  public OOOProtocolMessage createAckMessage(long sequence);
  public void sendAck(long sequence);
  
  public void sendMessage(OOOProtocolMessage msg);

  public void receiveMessage(OOOProtocolMessage msg);
  
  public OOOProtocolMessage createProtocolMessage(long sent, TCNetworkMessage msg);
  
}