/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Default Health Checker which is tied to the communications manager. All it does is, attaching a ECHO context to the
 * ESTABLISHED TC Connection
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerEchoImpl implements ConnectionHealthChecker {
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
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextEchoImpl(transportBase);
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
