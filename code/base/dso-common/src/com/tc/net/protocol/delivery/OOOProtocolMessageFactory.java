/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCNetworkMessage;

interface OOOProtocolMessageFactory {
  public OOOProtocolMessage createNewAckRequestMessage();
  
  public OOOProtocolMessage createNewAckMessage(long sequence);
  
  public OOOProtocolMessage createNewSendMessage(long sequence, TCNetworkMessage payload);
  
  public OOOProtocolMessage createNewMessage(OOOProtocolMessageHeader header, TCByteBuffer[] data);
}
