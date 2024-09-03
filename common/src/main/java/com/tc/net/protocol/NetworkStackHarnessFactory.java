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
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;

public interface NetworkStackHarnessFactory {

  /**
   * Creates server-side stack harnesses.
   * 
   * @param transportListeners An array of MessageTransportListeners that ought to be wired up to the transport (in
   *        addition to any that might be created by the stack harness)
   */
  ServerNetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                                          MessageTransportListener[] transportListeners);

  /**
   * Creates client-side stack harnesses.
   */
  ClientNetworkStackHarness createClientHarness(MessageTransportFactory transportFactory, ClientMessageChannel channel,
                                          MessageTransportListener[] transportListeners);

}
