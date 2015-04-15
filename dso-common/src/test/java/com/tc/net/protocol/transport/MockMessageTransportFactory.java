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
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

import java.util.List;

public class MockMessageTransportFactory implements MessageTransportFactory {

  public MessageTransport transport;
  public int              callCount;

  @Override
  public MessageTransport createNewTransport() {
    callCount++;
    return transport;
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionID);
    return transport;
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List transportListeners) {
    callCount++;
    if (transport != null) transport.initConnectionID(connectionId);
    return transport;
  }
}
