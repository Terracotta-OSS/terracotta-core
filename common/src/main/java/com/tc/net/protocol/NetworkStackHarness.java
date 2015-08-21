/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.transport.MessageTransport;

public interface NetworkStackHarness {

  public MessageTransport attachNewConnection(TCConnection connection) throws IllegalReconnectException;

  public void finalizeStack();

}
