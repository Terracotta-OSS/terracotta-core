/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.transport.MessageTransport;

public interface NetworkStackHarness {

  public MessageTransport attachNewConnection(TCConnection connection) throws IllegalReconnectException;

  public void finalizeStack();

}
