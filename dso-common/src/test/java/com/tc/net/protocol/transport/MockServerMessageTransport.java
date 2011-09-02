/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public class MockServerMessageTransport extends ServerMessageTransport {
  public final NoExceptionLinkedQueue sendToConnectionCalls = new NoExceptionLinkedQueue();

  public MockServerMessageTransport(ConnectionID connectionId, TCConnection conn,
                                    TransportHandshakeErrorHandler handshakeErrorHandler,
                                    TransportHandshakeMessageFactory messageFactory) {
    super(connectionId, conn, handshakeErrorHandler, messageFactory);
  }

  @Override
  public void sendToConnection(TCNetworkMessage message) {
    sendToConnectionCalls.put(message);
  }

}
