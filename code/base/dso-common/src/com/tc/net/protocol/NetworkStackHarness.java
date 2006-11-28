/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.transport.MessageTransport;

public interface NetworkStackHarness {

  public MessageTransport attachNewConnection(TCConnection connection);

  public void finalizeStack();

}
