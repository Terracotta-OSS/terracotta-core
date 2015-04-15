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

/**
 * Listener for MessageTransport events
 */
public interface MessageTransportListener {

  /**
   * Called when the transport's connection is established.
   */
  public void notifyTransportConnected(MessageTransport transport);

  /**
   * Called when the transport's connection goes away
   * 
   * @param forcedDisconnect - when a node disconnects its transport on its own (say, when HC detects it as dead)
   */
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect);

  /**
   * Called when the transport tries to connect.
   */
  public void notifyTransportConnectAttempt(MessageTransport transport);

  /**
   * Called when the transport is closed.
   */
  public void notifyTransportClosed(MessageTransport transport);

  /**
   * Called when reconnection rejected by L2 and no more trying to reconnect.
   */
  public void notifyTransportReconnectionRejected(MessageTransport transport);
}
