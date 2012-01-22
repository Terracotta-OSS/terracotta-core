/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

import java.util.List;

public interface MessageTransportFactory {

  MessageTransport createNewTransport();

  MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                      TransportHandshakeMessageFactory handshakeMessageFactory, List transportListeners);

  MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                      TransportHandshakeErrorHandler handler,
                                      TransportHandshakeMessageFactory handshakeMessageFactory, List transportListeners);

}
