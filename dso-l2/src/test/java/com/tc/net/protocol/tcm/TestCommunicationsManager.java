/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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