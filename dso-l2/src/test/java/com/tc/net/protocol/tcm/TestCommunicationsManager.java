/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.SessionProvider;

public class TestCommunicationsManager implements CommunicationsManager {

  public boolean shutdown;

  @Override
  public TCConnectionManager getConnectionManager() {
    throw new ImplementMe();
  }

  @Override
  public void shutdown() {
    throw new ImplementMe();
  }

  @Override
  public NetworkListener[] getAllListeners() {
    throw new ImplementMe();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory, TCMessageFactory msgFactory) {
    throw new ImplementMe();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider) {
    throw new ImplementMe();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory) {
    throw new ImplementMe();
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory) {
    throw new ImplementMe();
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddress) {
    throw new ImplementMe();
  }

  @Override
  public boolean isInShutdown() {
    return this.shutdown;
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress address, boolean b,
                                        ConnectionIDFactory connectionIDFactory, Sink httpSink) {
    throw new ImplementMe();
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory,
                                        WireProtocolMessageSink wireProtoMsgSink) {
    throw new ImplementMe();
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class messageClass) {
    throw new ImplementMe();

  }

  @Override
  public void addClassMapping(TCMessageType messageType, GeneratedMessageFactory generatedMessageFactory) {
    throw new ImplementMe();

  }

}