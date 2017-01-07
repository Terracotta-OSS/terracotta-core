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

/**
 * CommsMgr provides Listener and Channel endpoints for exchanging <code>TCMessage</code> type messages
 */
public interface CommunicationsManager {

  static final String COMMSMGR_GROUPS = "L2_L2";
  static final String COMMSMGR_SERVER = "L2_L1";
  static final String COMMSMGR_CLIENT = "L1_L2";

  public TCConnectionManager getConnectionManager();

  public void shutdown();

  public boolean isInShutdown();

  public NetworkListener[] getAllListeners();

  public void addClassMapping(TCMessageType messageType, Class<? extends TCMessage> messageClass);

  /**
   * Creates a client message channel to the given host/port.
   * 
   * @param maxReconnectTries The number of times the channel will attempt to reestablish communications with the server
   *        if the connection is lost. If n==0, the channel will not attempt to reestablish communications. If n>0, the
   *        channel will attempt to reestablish communications n times. If n<0 the channel will always try to
   *        reestablish communications.
   * @param hostname The hostname to connect to.
   * @param port The remote port to connect to.
   * @param timeout The maximum time (in milliseconds) to wait for the underlying connection to be established before
   *        giving up.
   */

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  Collection<ConnectionInfo> addressProvider,
                                                  MessageTransportFactory transportFactory, TCMessageFactory msgFactory);

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,                                                   
                                                  Collection<ConnectionInfo> connections);

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  Collection<ConnectionInfo> addressProvider,
                                                  MessageTransportFactory transportFactory);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory,
                                        WireProtocolMessageSink wireProtoMsgSink);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddress);

}
