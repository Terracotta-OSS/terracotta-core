/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

import java.util.List;

public class MockMessageTransportFactory implements MessageTransportFactory {

  public MessageTransport transport;
  public int              callCount;

  public MessageTransport createNewTransport() {
    callCount++;
    return transport;
  }

  public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionID);
    return transport;
  }

  public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionId);
    return transport;
  }
}
