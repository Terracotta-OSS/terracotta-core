/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ConnectionWatcher implements MessageTransportListener {

  protected final ClientMessageTransport      cmt;
  protected final ClientConnectionEstablisher cce;
  protected final MessageTransportListener    target;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageTransport cmt, MessageTransportListener target, ClientConnectionEstablisher cce) {
    this.cmt = cmt;
    this.target = target;
    this.cce = cce;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
    target.notifyTransportClosed(transport);
  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    cce.asyncReconnect(cmt);
    target.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    target.notifyTransportConnectAttempt(transport);
  }

  public void notifyTransportConnected(MessageTransport transport) {
    target.notifyTransportConnected(transport);
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    target.notifyTransportReconnectionRejected(transport);
  }
}
