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
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.session.SessionProvider;
import com.tc.operatorevent.NodeNameProvider;
import com.tc.util.ProductID;

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
   * @param timeout The maximum time (in milliseconds) to wait for the underlying connection to be established before
   *        giving up.
   */

  public ClientMessageChannel createClientChannel(ProductID product, SessionProvider provider, int timeout);
    
  public NetworkListener createListener(TCSocketAddress addr, boolean transportDisconnectRemovesChannel, 
                                        ConnectionIDFactory connectionIdFactory);

  public NetworkListener createListener(TCSocketAddress addr, boolean transportDisconnectRemovesChannel, 
                                        NodeNameProvider activeNameProvider);
}
