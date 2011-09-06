/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
   * 
   * @param forcedDisconnect - when a node disconnects its transport on its own (say, when HC detects it as dead)
   */
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect);

  /**
   * Called when the transport tries to connect.
   */
  public void notifyTransportConnectAttempt(MessageTransport transport);

  /**
   * Called when the transport is closed.
   */
  public void notifyTransportClosed(MessageTransport transport);

  /**
   * Called when reconnection rejected by L2 and no more trying to reconnect.
   */
  public void notifyTransportReconnectionRejected(MessageTransport transport);
}
