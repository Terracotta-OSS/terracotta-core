/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.StackNotFoundException;

/**
 * Provider/locator for a network stack.
 */
public interface NetworkStackProvider {

  /**
   * Takes a new connection and a connectionId. Returns the MessageTransport associated with that id.
   */
  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection)
      throws StackNotFoundException, IllegalReconnectException;

}