/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import java.net.InetSocketAddress;
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
  public ClientMessageChannel createClientChannel(ProductID product, int timeout) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public NetworkListener createListener(InetSocketAddress addr, Predicate<MessageChannel> transportDisconnectRemovesChannel, ConnectionIDFactory connectionIdFactory, Predicate<MessageTransport> validate) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public NetworkListener createListener(InetSocketAddress addr, boolean transportDisconnectRemovesChannel, ConnectionIDFactory connectionIdFactory, RedirectAddressProvider activeNameProvider) {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public boolean isInShutdown() {
    return this.shutdown;
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class<? extends TCAction> messageClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

  
}