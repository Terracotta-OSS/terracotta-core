/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * A Dummy Connection HealthChecker. Ties a Dummy HealthChecker Context to a transport when it is connected.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerDummyImpl implements ConnectionHealthChecker {
  private MessageTransportBase transportBase;

  public void start() {
    // keep mum
  }

  public void stop() {
    // keep mum
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // who cares
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // who cares
  }

  public void notifyTransportConnected(MessageTransport transport) {
    this.transportBase = (MessageTransportBase) transport;
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextDummyImpl();
    transportBase.setHealthCheckerContext(context);
  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    // who cares
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

}
