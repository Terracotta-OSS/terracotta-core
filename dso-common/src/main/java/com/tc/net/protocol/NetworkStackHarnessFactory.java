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
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.MessageChannelInternal;
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
  NetworkStackHarness createServerHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                                          MessageTransportListener[] transportListeners);

  /**
   * Creates client-side stack harnesses.
   */
  NetworkStackHarness createClientHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel,
                                          MessageTransportListener[] transportListeners);

}
