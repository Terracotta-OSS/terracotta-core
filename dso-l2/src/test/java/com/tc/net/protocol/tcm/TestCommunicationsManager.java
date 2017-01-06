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

import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.SessionProvider;
import java.util.Collection;

public class TestCommunicationsManager implements CommunicationsManager {

  public boolean shutdown;

  @Override
  public TCConnectionManager getConnectionManager() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkListener[] getAllListeners() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  Collection<ConnectionInfo> addressProvider,
                                                  MessageTransportFactory transportFactory, TCMessageFactory msgFactory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  Collection<ConnectionInfo> addressProvider) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  Collection<ConnectionInfo> addressProvider,
                                                  MessageTransportFactory transportFactory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddress) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInShutdown() {
    return this.shutdown;
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory,
                                        WireProtocolMessageSink wireProtoMsgSink) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class<? extends TCMessage> messageClass) {
    throw new UnsupportedOperationException();

  }

}