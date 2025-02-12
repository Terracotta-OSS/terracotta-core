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

import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.core.ProductID;
import com.tc.text.PrettyPrintable;
import java.net.InetSocketAddress;
import java.util.function.Predicate;

/**
 * CommsMgr provides Listener and Channel endpoints for exchanging <code>TCMessage</code> type messages
 */
public interface CommunicationsManager extends PrettyPrintable {

  static final String COMMSMGR_GROUPS = "L2_L2";
  static final String COMMSMGR_SERVER = "L2_L1";
  static final String COMMSMGR_CLIENT = "L1_L2";

  public TCConnectionManager getConnectionManager();

  public void shutdown();

  public boolean isInShutdown();

  public NetworkListener[] getAllListeners();

  public void addClassMapping(TCMessageType messageType, Class<? extends TCAction> messageClass);

  /**
   * Creates a client message channel to the given host/port.
   * 
   * @param timeout The maximum time (in milliseconds) to wait for the underlying connection to be established before
   *        giving up.
   */

  public ClientMessageChannel createClientChannel(ProductID product, int timeout);
    
  public NetworkListener createListener(InetSocketAddress addr, Predicate<MessageChannel> transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, Predicate<MessageTransport> validation);

  public NetworkListener createListener(InetSocketAddress addr, boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, RedirectAddressProvider activeNameProvider);
}
