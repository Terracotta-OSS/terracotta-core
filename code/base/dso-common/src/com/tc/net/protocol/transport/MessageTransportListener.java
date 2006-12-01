/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Listener for MessageTransport events
 */
public interface MessageTransportListener {
  
  /**
   * Called when the transport's connection is established.
   */
  public void notifyTransportConnected(MessageTransport transport);
  
  /**
   * Called when the transport's connection goes away
   */
  public void notifyTransportDisconnected(MessageTransport transport);

  /**
   * Called when the transport tries to connect.
   */
  public void notifyTransportConnectAttempt(MessageTransport transport);
  
  /**
   * Called when the transport is closed.
   */
  public void notifyTransportClosed(MessageTransport transport);
}
