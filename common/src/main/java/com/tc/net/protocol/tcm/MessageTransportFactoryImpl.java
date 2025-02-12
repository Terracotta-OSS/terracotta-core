/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.ServerMessageTransport;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessage;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;

import java.util.List;

public class MessageTransportFactoryImpl implements MessageTransportFactory {
  private final TransportHandshakeMessageFactory transportMessageFactory;
  private final ConnectionHealthChecker          connectionHealthChecker;
  private final TCConnectionManager              connectionMgr;
  private final int                              timeout;
  private final TransportHandshakeErrorHandler   defaultHandshakeErrorHandler;
  private final ReconnectionRejectedHandler      reconnectionRejectedHandler;

  public MessageTransportFactoryImpl(TransportHandshakeMessageFactory transportMessageFactory,
                                     ConnectionHealthChecker connectionHealthChecker,
                                     TCConnectionManager connectionManager,
                                     int timeout, 
                                     TransportHandshakeErrorHandler defaultHandshakeErrorHandler,
                                     ReconnectionRejectedHandler reconnectionRejectedBehaviour) {
    this.transportMessageFactory = transportMessageFactory;
    this.connectionHealthChecker = connectionHealthChecker;
    this.connectionMgr = connectionManager;
    this.timeout = timeout;
    this.defaultHandshakeErrorHandler = defaultHandshakeErrorHandler;
    this.reconnectionRejectedHandler = reconnectionRejectedBehaviour;
  }

  @Override
  public ClientMessageTransport createNewTransport() {
    ClientMessageTransport cmt = createClientMessageTransport(
                                                              defaultHandshakeErrorHandler, transportMessageFactory,
                                                              new WireProtocolAdaptorFactoryImpl());
    cmt.addTransportListener(connectionHealthChecker);
    return cmt;
  }

  protected ClientMessageTransport createClientMessageTransport(TransportHandshakeErrorHandler handshakeErrorHandler,
                                                                TransportHandshakeMessageFactory messageFactory,
                                                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory) {
    return new ClientMessageTransport(this.connectionMgr, handshakeErrorHandler, transportMessageFactory,
                                      wireProtocolAdaptorFactory, this.timeout, reconnectionRejectedHandler);
  }

  @Override
  public ServerMessageTransport createNewTransport(TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List<MessageTransportListener> transportListeners) {
    throw new AssertionError();
  }

  @Override
  public ServerMessageTransport createNewTransport(TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List<MessageTransportListener> transportListeners) {
    throw new AssertionError();
  }

}
