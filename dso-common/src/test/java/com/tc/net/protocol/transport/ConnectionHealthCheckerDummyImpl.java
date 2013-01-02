/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * A Dummy Connection HealthChecker. Ties a Dummy HealthChecker Context to a transport when it is connected.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerDummyImpl implements ConnectionHealthChecker {
  private MessageTransportBase transportBase;

  @Override
  public void start() {
    // keep mum
  }

  @Override
  public void stop() {
    // keep mum
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    // who cares
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // who cares
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    this.transportBase = (MessageTransportBase) transport;
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextDummyImpl();
    transportBase.setHealthCheckerContext(context);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    // who cares
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

}
