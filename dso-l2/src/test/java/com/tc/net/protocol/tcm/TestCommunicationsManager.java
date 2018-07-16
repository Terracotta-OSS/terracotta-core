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
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.object.session.SessionProvider;
import com.tc.operatorevent.NodeNameProvider;
import com.tc.util.ProductID;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

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
  public ClientMessageChannel createClientChannel(ProductID product, SessionProvider provider, int timeout) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public NetworkListener createListener(TCSocketAddress addr, boolean transportDisconnectRemovesChannel, ConnectionIDFactory connectionIdFactory, Predicate<MessageTransport> validate) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public NetworkListener createListener(TCSocketAddress addr, boolean transportDisconnectRemovesChannel, ConnectionIDFactory connectionIdFactory, NodeNameProvider activeNameProvider) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public boolean isInShutdown() {
    return this.shutdown;
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class<? extends TCMessage> messageClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

  
}