/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;

import java.util.List;

public class MessageTransportFactoryImpl implements MessageTransportFactory {
  private final TransportHandshakeMessageFactory transportMessageFactory;
  private final ConnectionHealthChecker          connectionHealthChecker;
  private final TCConnectionManager              connectionMgr;
  private final ConnectionAddressProvider        addressProvider;
  private final int                              maxReconnectTries;
  private final int                              timeout;
  private final int                              callbackport;
  private final TransportHandshakeErrorHandler   defaultHandshakeErrorHandler;
  private final ReconnectionRejectedHandler      reconnectionRejectedHandler;

  public MessageTransportFactoryImpl(final TransportHandshakeMessageFactory transportMessageFactory,
                                     final ConnectionHealthChecker connectionHealthChecker,
                                     final TCConnectionManager connectionManager,
                                     final ConnectionAddressProvider addressProvider, final int maxReconnectTries,
                                     final int timeout, int callbackPort,
                                     TransportHandshakeErrorHandler defaultHandshakeErrorHandler,
                                     ReconnectionRejectedHandler reconnectionRejectedBehaviour) {
    this.transportMessageFactory = transportMessageFactory;
    this.connectionHealthChecker = connectionHealthChecker;
    this.connectionMgr = connectionManager;
    this.addressProvider = addressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;
    this.callbackport = callbackPort;
    this.defaultHandshakeErrorHandler = defaultHandshakeErrorHandler;
    this.reconnectionRejectedHandler = reconnectionRejectedBehaviour;
  }

  public MessageTransport createNewTransport() {
    ClientConnectionEstablisher clientConnectionEstablisher = new ClientConnectionEstablisher(connectionMgr,
                                                                                              addressProvider,
                                                                                              maxReconnectTries,
                                                                                              timeout);
    ClientMessageTransport cmt = createClientMessageTransport(clientConnectionEstablisher,
                                                              defaultHandshakeErrorHandler, transportMessageFactory,
                                                              new WireProtocolAdaptorFactoryImpl(), callbackport);
    cmt.addTransportListener(connectionHealthChecker);
    return cmt;
  }

  protected ClientMessageTransport createClientMessageTransport(ClientConnectionEstablisher clientConnectionEstablisher,
                                                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                                                TransportHandshakeMessageFactory messageFactory,
                                                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                                                                int callbackPortNum) {
    return new ClientMessageTransport(clientConnectionEstablisher, handshakeErrorHandler, transportMessageFactory,
                                      wireProtocolAdaptorFactory, callbackPortNum, reconnectionRejectedHandler);
  }

  public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    throw new AssertionError();
  }

  public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    throw new AssertionError();
  }

}
