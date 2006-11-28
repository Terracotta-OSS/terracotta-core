/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCProtocolException;

/**
 * Parses incoming network data into ProtocolMessages
 */
interface OOOProtocolMessageParser {
  public OOOProtocolMessage parseMessage(TCByteBuffer[] data) throws TCProtocolException;
}
