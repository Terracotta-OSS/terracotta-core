/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.ServerMessageTransport;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;

import java.util.List;

public class MessageTransportFactoryImpl implements MessageTransportFactory {
  private final TransportHandshakeMessageFactory transportMessageFactory;
  private final ConnectionHealthChecker          connectionHealthChecker;
  private final TCConnectionManager              connectionMgr;
  private final int                              timeout;
  private final int                              callbackport;
  private final TransportHandshakeErrorHandler   defaultHandshakeErrorHandler;
  private final ReconnectionRejectedHandler      reconnectionRejectedHandler;

  public MessageTransportFactoryImpl(TransportHandshakeMessageFactory transportMessageFactory,
                                     ConnectionHealthChecker connectionHealthChecker,
                                     TCConnectionManager connectionManager,
                                     int timeout, int callbackPort,
                                     TransportHandshakeErrorHandler defaultHandshakeErrorHandler,
                                     ReconnectionRejectedHandler reconnectionRejectedBehaviour) {
    this.transportMessageFactory = transportMessageFactory;
    this.connectionHealthChecker = connectionHealthChecker;
    this.connectionMgr = connectionManager;
    this.timeout = timeout;
    this.callbackport = callbackPort;
    this.defaultHandshakeErrorHandler = defaultHandshakeErrorHandler;
    this.reconnectionRejectedHandler = reconnectionRejectedBehaviour;
  }
  
  @Override
  public ClientConnectionEstablisher createClientConnectionEstablisher() {
    ClientConnectionEstablisher clientConnectionEstablisher = new ClientConnectionEstablisher(reconnectionRejectedHandler);
    return clientConnectionEstablisher;
  }

  @Override
  public ClientMessageTransport createNewTransport() {
    ClientMessageTransport cmt = createClientMessageTransport(
                                                              defaultHandshakeErrorHandler, transportMessageFactory,
                                                              new WireProtocolAdaptorFactoryImpl(), callbackport);
    cmt.addTransportListener(connectionHealthChecker);
    return cmt;
  }

  protected ClientMessageTransport createClientMessageTransport(TransportHandshakeErrorHandler handshakeErrorHandler,
                                                                TransportHandshakeMessageFactory messageFactory,
                                                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                                                                int callbackPortNum) {
    return new ClientMessageTransport(this.connectionMgr, handshakeErrorHandler, transportMessageFactory,
                                      wireProtocolAdaptorFactory, callbackPortNum, this.timeout, reconnectionRejectedHandler);
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
